package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.ServiceReviewRequest;
import com.raf.zuhoo.data.model.response.ServiceReviewResponse;

import retrofit2.Callback;

public class ReviewRepository {

    private final ApiService apiService;

    public ReviewRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void submitReview(Long serviceRequestId, int rating, String comment,
                             Callback<ServiceReviewResponse> callback) {
        apiService.submitReview(new ServiceReviewRequest(serviceRequestId, rating, comment))
                .enqueue(callback);
    }
}
