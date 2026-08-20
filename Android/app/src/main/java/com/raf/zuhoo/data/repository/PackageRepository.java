package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.SubscribeRequest;
import com.raf.zuhoo.data.model.response.ServicePackageResponse;
import com.raf.zuhoo.data.model.response.SubscriptionSummary;

import java.util.List;

import retrofit2.Callback;

public class PackageRepository {

    private final ApiService apiService;

    public PackageRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getActivePackages(Callback<List<ServicePackageResponse>> callback) {
        apiService.getActivePackages().enqueue(callback);
    }

    public void subscribe(Long packageId, Boolean autoRenew, Callback<SubscriptionSummary> callback) {
        apiService.subscribe(new SubscribeRequest(packageId, autoRenew)).enqueue(callback);
    }
}
