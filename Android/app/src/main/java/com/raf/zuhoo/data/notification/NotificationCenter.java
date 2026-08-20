package com.raf.zuhoo.data.notification;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.raf.zuhoo.data.chat.ChatSocket;
import com.raf.zuhoo.data.local.TokenManager;
import com.raf.zuhoo.data.model.response.NotificationCountResponse;
import com.raf.zuhoo.data.repository.NotificationRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * App-wide unread notification count.
 *
 * The backend pushes every notification live on /user/queue/notifications — a destination the
 * original plan didn't know about — so the badge can stay current without polling. The REST count
 * seeds it (and re-seeds after a reconnect, since anything pushed while the socket was down was
 * missed); the socket keeps it moving after that.
 *
 * This is distinct from FCM: this is what updates the badge while the user is *in* the app.
 */
public class NotificationCenter {

    private static final String DESTINATION = "/user/queue/notifications";

    private final NotificationRepository notificationRepository;
    private final ChatSocket chatSocket;
    private final TokenManager tokenManager;

    private final MutableLiveData<Long> unreadCount = new MutableLiveData<>(0L);

    private ChatSocket.Subscription subscription;

    public NotificationCenter(Context context, ChatSocket chatSocket, TokenManager tokenManager) {
        this.notificationRepository = new NotificationRepository(context);
        this.chatSocket = chatSocket;
        this.tokenManager = tokenManager;
    }

    public LiveData<Long> unreadCount() {
        return unreadCount;
    }

    /** Idempotent — safe to call from every screen that shows the badge. */
    public void start() {

        if (!tokenManager.isLoggedIn()) {
            return;
        }

        refresh();

        if (subscription != null) {
            return;
        }

        // Only the arrival matters, not the payload — the count is authoritative from the server
        // and a re-read keeps this honest if several notifications land at once.
        subscription = chatSocket.subscribe(DESTINATION, (destination, jsonBody) -> refresh());
    }

    public void refresh() {

        if (!tokenManager.isLoggedIn()) {
            unreadCount.setValue(0L);
            return;
        }

        notificationRepository.getUnreadCount(new Callback<NotificationCountResponse>() {

            @Override
            public void onResponse(Call<NotificationCountResponse> call,
                                   Response<NotificationCountResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    unreadCount.setValue(response.body().getUnreadCount());
                }
            }

            @Override
            public void onFailure(Call<NotificationCountResponse> call, Throwable t) {
                // Leave the last known count rather than showing a wrong zero.
            }
        });
    }

    /** Called on logout, so the next account doesn't inherit a stale badge. */
    public void stop() {

        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }

        unreadCount.setValue(0L);
    }
}
