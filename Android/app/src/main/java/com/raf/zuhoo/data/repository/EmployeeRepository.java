package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.EmployeeResponse;
import com.raf.zuhoo.data.model.response.PageResponse;

import retrofit2.Callback;

public class EmployeeRepository {

    private final ApiService apiService;

    public EmployeeRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getEmployees(Callback<PageResponse<EmployeeResponse>> callback) {
        apiService.getEmployees().enqueue(callback);
    }
}
