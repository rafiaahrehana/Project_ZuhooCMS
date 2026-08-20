package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.PublicClientRegisterRequest;
import com.raf.zuhoo.data.model.response.ClientResponse;

import retrofit2.Callback;

public class ClientRepository {

    private final ApiService apiService;

    public ClientRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void registerPublic(PublicClientRegisterRequest request, Callback<ClientResponse> callback) {
        apiService.registerClient(request).enqueue(callback);
    }
}
