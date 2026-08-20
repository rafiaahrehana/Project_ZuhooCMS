package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.UpdateLeadStatusRequest;
import com.raf.zuhoo.data.model.response.LeadResponse;
import com.raf.zuhoo.data.model.response.PageResponse;

import retrofit2.Callback;

public class LeadRepository {

    private final ApiService apiService;

    public LeadRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMyLeads(Callback<PageResponse<LeadResponse>> callback) {
        apiService.getMyLeads().enqueue(callback);
    }

    public void updateLeadStatus(Long id, String status, Callback<LeadResponse> callback) {
        apiService.updateLeadStatus(id, new UpdateLeadStatusRequest(status)).enqueue(callback);
    }
}
