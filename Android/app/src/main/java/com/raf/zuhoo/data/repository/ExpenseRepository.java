package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.ExpenseStatus;
import com.raf.zuhoo.data.model.request.CreateExpenseRequest;
import com.raf.zuhoo.data.model.response.ExpenseResponse;
import com.raf.zuhoo.data.model.response.PageResponse;

import java.math.BigDecimal;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class ExpenseRepository {

    private final ApiService apiService;

    public ExpenseRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMyExpenses(Callback<PageResponse<ExpenseResponse>> callback) {
        apiService.getMyExpenses().enqueue(callback);
    }

    public void createExpense(String description, BigDecimal amount, String category,
                              String vendorName, String expenseDate, String receiptUrl,
                              Callback<ExpenseResponse> callback) {
        apiService.createExpense(new CreateExpenseRequest(
                description, amount, category, vendorName, expenseDate, receiptUrl)).enqueue(callback);
    }

    public void getPendingExpenses(Callback<PageResponse<ExpenseResponse>> callback) {
        apiService.getExpensesByStatus(ExpenseStatus.PENDING).enqueue(callback);
    }

    public void approveExpense(Long id, String notes, Callback<ResponseBody> callback) {
        apiService.approveExpense(id, notes).enqueue(callback);
    }

    public void rejectExpense(Long id, String reason, Callback<ResponseBody> callback) {
        apiService.rejectExpense(id, reason).enqueue(callback);
    }
}
