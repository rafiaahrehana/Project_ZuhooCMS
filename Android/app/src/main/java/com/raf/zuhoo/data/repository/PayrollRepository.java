package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.response.EmployeeResponse;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.PayrollResponse;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class PayrollRepository {

    private final ApiService apiService;

    public PayrollRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    /**
     * There's no bare "my payslips" endpoint server-side — payroll is looked up by employeeId,
     * so the caller's own id has to be resolved first via /api/employees/me.
     */
    public void getMyEmployeeProfile(Callback<EmployeeResponse> callback) {
        apiService.getMyEmployeeProfile().enqueue(callback);
    }

    public void getMyPayslips(Long employeeId, Callback<PageResponse<PayrollResponse>> callback) {
        apiService.getMyPayslips(employeeId).enqueue(callback);
    }

    public void downloadPayslipPdf(Long id, Callback<ResponseBody> callback) {
        apiService.downloadPayslipPdf(id).enqueue(callback);
    }
}
