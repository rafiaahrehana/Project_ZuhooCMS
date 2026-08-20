package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.SubscriptionSummary;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class SubscriptionRepository {

    private final ApiService apiService;

    public SubscriptionRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMySubscriptions(Callback<PageResponse<SubscriptionSummary>> callback) {
        apiService.getMySubscriptions().enqueue(callback);
    }

    public void cancelSubscription(Long id, String reason, Callback<ResponseBody> callback) {
        apiService.cancelSubscription(id, reason).enqueue(callback);
    }
}
