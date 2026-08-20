package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.CompanyServiceResponse;
import com.raf.zuhoo.data.model.response.ServiceCategoryResponse;
import com.raf.zuhoo.data.model.response.ServiceFormField;

import java.util.List;

import retrofit2.Callback;

public class CatalogRepository {

    private final ApiService apiService;

    public CatalogRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getActiveServices(Callback<List<CompanyServiceResponse>> callback) {
        apiService.getActiveServices().enqueue(callback);
    }

    public void getServiceCategories(Callback<List<ServiceCategoryResponse>> callback) {
        apiService.getServiceCategories().enqueue(callback);
    }

    public void getServiceFormFields(Long serviceId, Callback<List<ServiceFormField>> callback) {
        apiService.getServiceFormFields(serviceId).enqueue(callback);
    }
}
