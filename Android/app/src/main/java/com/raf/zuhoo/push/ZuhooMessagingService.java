package com.raf.zuhoo.push;

import android.app.PendingIntent;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.raf.zuhoo.R;
import com.raf.zuhoo.data.local.PushTokenStore;
import com.raf.zuhoo.data.local.TokenManager;
import com.raf.zuhoo.data.repository.DeviceTokenRepository;

import java.util.Map;

public class ZuhooMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        // FCM reissues tokens on reinstall, restore, and periodically on its own schedule. Store
        // it first so it survives even if the user is signed out right now — registerIfNeeded()
        // no-ops without a session and runs again at next login.
        new PushTokenStore(this).setToken(token);
        new DeviceTokenRepository(this).registerIfNeeded();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // A signed-out device should not be showing anyone's notifications. This can happen
        // briefly if a push was already in flight when the user logged out.
        if (!new TokenManager(this).isLoggedIn()) {
            return;
        }

        Map<String, String> data = remoteMessage.getData();

        // The backend sends data-only messages so the client builds the notification itself —
        // that keeps the channel, the deep link and (later) localisation on this side.
        String type = data.get("type");
        String title = data.get("title");
        String body = data.get("body");

        if (TextUtils.isEmpty(title) && TextUtils.isEmpty(body)) {
            return;
        }

        show(type, title, body, data.get("serviceRequestId"), data.get("notificationId"));
    }

    private void show(String type, String title, String body, String serviceRequestId,
                      String notificationId) {

        Intent intent = PushRouter.intentFor(this, type, serviceRequestId);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode(notificationId),
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, PushChannels.forType(type))
                        // A flat white silhouette, not the launcher icon. Android strips colour
                        // from the status-bar icon and keeps only the alpha, so a full-colour
                        // icon renders as an unrecognisable white blob.
                        .setSmallIcon(R.drawable.ic_notification)
                        .setColor(ContextCompat.getColor(this, R.color.notification_accent))
                        .setContentTitle(TextUtils.isEmpty(title) ? getString(R.string.app_name) : title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        try {
            manager.notify(requestCode(notificationId), builder.build());
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS was denied (API 33+). Nothing to do — the notification is
            // already persisted server-side and shows up in the in-app list.
        }
    }

    // Distinct ids per notification so several can stack rather than overwrite each other,
    // falling back to a single slot when the backend didn't send one.
    private int requestCode(String notificationId) {

        if (notificationId == null || notificationId.isEmpty()) {
            return 0;
        }

        try {
            return (int) Long.parseLong(notificationId);
        } catch (NumberFormatException e) {
            return notificationId.hashCode();
        }
    }
}
