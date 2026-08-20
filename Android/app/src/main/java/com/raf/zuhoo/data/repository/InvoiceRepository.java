package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.InvoiceSummary;
import com.raf.zuhoo.data.model.response.PageResponse;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class InvoiceRepository {

    private final ApiService apiService;

    public InvoiceRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMyInvoices(Callback<PageResponse<InvoiceSummary>> callback) {
        apiService.getMyInvoices().enqueue(callback);
    }

    public void getInvoice(Long id, Callback<InvoiceSummary> callback) {
        apiService.getInvoice(id).enqueue(callback);
    }

    public void downloadPdf(Long id, Callback<ResponseBody> callback) {
        apiService.downloadInvoicePdf(id).enqueue(callback);
    }
}
