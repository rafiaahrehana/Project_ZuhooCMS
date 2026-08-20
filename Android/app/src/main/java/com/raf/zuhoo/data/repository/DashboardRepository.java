package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.ClientSummaryResponse;

import retrofit2.Callback;

public class DashboardRepository {

    private final ApiService apiService;

    public DashboardRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getClientSummary(Callback<ClientSummaryResponse> callback) {
        apiService.getClientSummary().enqueue(callback);
    }
}
