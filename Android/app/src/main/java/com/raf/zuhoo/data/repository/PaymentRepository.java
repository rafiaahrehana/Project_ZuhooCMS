package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.InitiatePaymentRequest;

import java.math.BigDecimal;
import java.util.Map;

import retrofit2.Callback;

public class PaymentRepository {

    public static final String PURPOSE_INVOICE = "INVOICE";
    public static final String PURPOSE_PACKAGE_SUBSCRIPTION = "PACKAGE_SUBSCRIPTION";

    private final ApiService apiService;

    public PaymentRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void initiate(String purpose, Long targetId, BigDecimal amount,
                         Callback<Map<String, String>> callback) {
        apiService.initiatePayment(new InitiatePaymentRequest(purpose, targetId, amount))
                .enqueue(callback);
    }
}
