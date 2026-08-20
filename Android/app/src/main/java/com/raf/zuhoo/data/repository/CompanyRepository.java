package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.CompanyPublicResponse;

import java.util.List;

import retrofit2.Callback;

public class CompanyRepository {

    private final ApiService apiService;

    public CompanyRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getPublicCompanies(Callback<List<CompanyPublicResponse>> callback) {
        apiService.getPublicCompanies().enqueue(callback);
    }
}
