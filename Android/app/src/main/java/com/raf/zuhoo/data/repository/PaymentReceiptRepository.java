package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.PaymentReceiptSummary;

import retrofit2.Callback;

public class PaymentReceiptRepository {

    private final ApiService apiService;

    public PaymentReceiptRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMyReceipts(Callback<PageResponse<PaymentReceiptSummary>> callback) {
        apiService.getMyPaymentReceipts().enqueue(callback);
    }
}
