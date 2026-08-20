package com.raf.zuhoo.ui.notification;

import android.app.Application;

import androidx.annotation.NonNull;

import com.raf.zuhoo.R;
import com.raf.zuhoo.data.local.db.ListCache;
import com.raf.zuhoo.data.model.response.NotificationResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.repository.NotificationRepository;
import com.raf.zuhoo.ui.common.CachedListViewModel;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationListViewModel extends CachedListViewModel<NotificationResponse> {

    private final NotificationRepository repository;

    public NotificationListViewModel(@NonNull Application application) {
        super(application, ListCache.NOTIFICATIONS, NotificationResponse.class);
        repository = new NotificationRepository(application);
    }

    @Override
    protected void fetch(Callback<PageResponse<NotificationResponse>> callback) {
        repository.getNotifications(false, callback);
    }

    @Override
    protected Long idOf(NotificationResponse item) {
        return item.getId();
    }

    @Override
    protected int loadErrorRes() {
        return R.string.error_notifications_load_failed;
    }

    public void markRead(Long id) {

        repository.markRead(id, new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                refresh();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // The user has already been taken to the notification's content; a failed
                // read-receipt isn't worth interrupting them for.
            }
        });
    }

    public void markAllRead() {

        repository.markAllRead(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                refresh();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                refresh();
            }
        });
    }
}
