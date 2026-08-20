package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.UpdateMyClientProfileRequest;
import com.raf.zuhoo.data.model.response.ClientResponse;

import retrofit2.Callback;

public class ClientProfileRepository {

    private final ApiService apiService;

    public ClientProfileRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMyProfile(Callback<ClientResponse> callback) {
        apiService.getMyClientProfile().enqueue(callback);
    }

    public void updateMyProfile(String clientCompanyName, String industry, String website,
                                String billingAddress, String shippingAddress,
                                Callback<ClientResponse> callback) {
        apiService.updateMyClientProfile(new UpdateMyClientProfileRequest(
                clientCompanyName, industry, website, billingAddress, shippingAddress)).enqueue(callback);
    }
}
