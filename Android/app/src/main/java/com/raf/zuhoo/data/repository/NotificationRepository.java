package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.UpdateNotificationPreferenceRequest;
import com.raf.zuhoo.data.model.response.NotificationCountResponse;
import com.raf.zuhoo.data.model.response.NotificationPreferenceResponse;
import com.raf.zuhoo.data.model.response.NotificationResponse;
import com.raf.zuhoo.data.model.response.PageResponse;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class NotificationRepository {

    private final ApiService apiService;

    public NotificationRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getNotifications(boolean unreadOnly, Callback<PageResponse<NotificationResponse>> callback) {
        apiService.getNotifications(unreadOnly).enqueue(callback);
    }

    public void getUnreadCount(Callback<NotificationCountResponse> callback) {
        apiService.getNotificationCount().enqueue(callback);
    }

    public void markRead(Long id, Callback<ResponseBody> callback) {
        apiService.markNotificationRead(id).enqueue(callback);
    }

    public void markAllRead(Callback<ResponseBody> callback) {
        apiService.markAllNotificationsRead().enqueue(callback);
    }

    public void getPreferences(Callback<NotificationPreferenceResponse> callback) {
        apiService.getNotificationPreferences().enqueue(callback);
    }

    public void updatePreferences(UpdateNotificationPreferenceRequest request,
                                  Callback<NotificationPreferenceResponse> callback) {
        apiService.updateNotificationPreferences(request).enqueue(callback);
    }
}
