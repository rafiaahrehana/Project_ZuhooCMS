package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.FinanceDashboardResponse;
import com.raf.zuhoo.data.model.response.HrDashboardResponse;

import retrofit2.Callback;

public class CompanyOverviewRepository {

    private final ApiService apiService;

    public CompanyOverviewRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getFinanceDashboard(Callback<FinanceDashboardResponse> callback) {
        apiService.getFinanceDashboard().enqueue(callback);
    }

    public void getHrDashboard(Callback<HrDashboardResponse> callback) {
        apiService.getHrDashboard().enqueue(callback);
    }
}
