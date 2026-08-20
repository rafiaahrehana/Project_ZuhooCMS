package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.GlobalSearchResponse;

import retrofit2.Callback;

public class SearchRepository {

    private final ApiService apiService;

    public SearchRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void search(String query, Callback<GlobalSearchResponse> callback) {
        apiService.search(query).enqueue(callback);
    }
}
