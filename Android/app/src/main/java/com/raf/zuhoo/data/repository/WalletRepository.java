package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.WalletResponse;
import com.raf.zuhoo.data.model.response.WalletTransactionResponse;

import retrofit2.Callback;

public class WalletRepository {

    private final ApiService apiService;

    public WalletRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getWallet(Callback<WalletResponse> callback) {
        apiService.getWallet().enqueue(callback);
    }

    public void getTransactions(Callback<PageResponse<WalletTransactionResponse>> callback) {
        apiService.getWalletTransactions().enqueue(callback);
    }
}
