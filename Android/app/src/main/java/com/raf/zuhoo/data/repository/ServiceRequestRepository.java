package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.AddCommentRequest;
import com.raf.zuhoo.data.model.request.ChangeRequestStatusRequest;
import com.raf.zuhoo.data.model.request.CreateServiceRequestRequest;
import com.raf.zuhoo.data.model.request.RejectQuotationRequest;
import com.raf.zuhoo.data.model.request.SubmitQuotationRequest;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.RequestComment;
import com.raf.zuhoo.data.model.response.RequestStatusHistory;
import com.raf.zuhoo.data.model.response.ServiceRequestDetail;
import com.raf.zuhoo.data.model.response.ServiceRequestSummary;

import java.math.BigDecimal;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class ServiceRequestRepository {

    private final ApiService apiService;

    public ServiceRequestRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMyServiceRequests(Callback<PageResponse<ServiceRequestSummary>> callback) {
        apiService.getMyServiceRequests().enqueue(callback);
    }

    public void getServiceRequest(Long id, Callback<ServiceRequestDetail> callback) {
        apiService.getServiceRequest(id).enqueue(callback);
    }

    public void createServiceRequest(CreateServiceRequestRequest request,
                                     Callback<ServiceRequestDetail> callback) {
        apiService.createServiceRequest(request).enqueue(callback);
    }

    public void cancelServiceRequest(Long id, Callback<ResponseBody> callback) {
        apiService.cancelServiceRequest(id).enqueue(callback);
    }

    public void getHistory(Long id, Callback<List<RequestStatusHistory>> callback) {
        apiService.getRequestHistory(id).enqueue(callback);
    }

    public void getComments(Long id, Callback<PageResponse<RequestComment>> callback) {
        apiService.getComments(id).enqueue(callback);
    }

    public void addComment(Long id, AddCommentRequest request, Callback<RequestComment> callback) {
        apiService.addComment(id, request).enqueue(callback);
    }

    public void acceptQuotation(Long id, Callback<ServiceRequestDetail> callback) {
        apiService.acceptQuotation(id).enqueue(callback);
    }

    public void rejectQuotation(Long id, String reason, Callback<ServiceRequestDetail> callback) {
        apiService.rejectQuotation(id, new RejectQuotationRequest(reason)).enqueue(callback);
    }

    public void getAllServiceRequests(String status, Callback<PageResponse<ServiceRequestSummary>> callback) {
        apiService.getAllServiceRequests(status).enqueue(callback);
    }

    public void getAssignedToMe(Callback<PageResponse<ServiceRequestSummary>> callback) {
        apiService.getAssignedToMe().enqueue(callback);
    }

    public void assignServiceRequest(Long id, Long employeeId, Callback<ServiceRequestDetail> callback) {
        apiService.assignServiceRequest(id, employeeId).enqueue(callback);
    }

    public void changeStatus(Long id, String status, String reason, Callback<ServiceRequestDetail> callback) {
        apiService.changeRequestStatus(id, new ChangeRequestStatusRequest(status, reason)).enqueue(callback);
    }

    public void submitQuotation(Long id, BigDecimal amount, String currency, String notes,
                                Callback<ServiceRequestDetail> callback) {
        apiService.submitQuotation(id, new SubmitQuotationRequest(amount, currency, notes)).enqueue(callback);
    }
}
