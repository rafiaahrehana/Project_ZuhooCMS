package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.LeaveRequestStatus;
import com.raf.zuhoo.data.model.request.CreateLeaveRequestRequest;
import com.raf.zuhoo.data.model.request.ReviewLeaveRequestRequest;
import com.raf.zuhoo.data.model.response.LeaveBalanceResponse;
import com.raf.zuhoo.data.model.response.LeaveRequestResponse;
import com.raf.zuhoo.data.model.response.PageResponse;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Callback;

public class LeaveRequestRepository {

    private final ApiService apiService;

    public LeaveRequestRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMyLeaveRequests(Callback<PageResponse<LeaveRequestResponse>> callback) {
        apiService.getMyLeaveRequests().enqueue(callback);
    }

    public void getMyBalances(Callback<List<LeaveBalanceResponse>> callback) {
        apiService.getMyLeaveBalances().enqueue(callback);
    }

    public void createLeaveRequest(String leaveType, String startDate, String endDate, String reason,
                                   Callback<LeaveRequestResponse> callback) {
        apiService.createLeaveRequest(new CreateLeaveRequestRequest(leaveType, startDate, endDate, reason))
                .enqueue(callback);
    }

    public void cancelLeaveRequest(Long id, Callback<ResponseBody> callback) {
        apiService.cancelLeaveRequest(id).enqueue(callback);
    }

    public void getPendingLeaveRequests(Callback<PageResponse<LeaveRequestResponse>> callback) {
        apiService.getLeaveRequestsByStatus(LeaveRequestStatus.PENDING).enqueue(callback);
    }

    public void approveLeaveRequest(Long id, Callback<LeaveRequestResponse> callback) {
        apiService.reviewLeaveRequest(id, new ReviewLeaveRequestRequest(LeaveRequestStatus.APPROVED, null))
                .enqueue(callback);
    }

    public void rejectLeaveRequest(Long id, String reason, Callback<LeaveRequestResponse> callback) {
        apiService.reviewLeaveRequest(id, new ReviewLeaveRequestRequest(LeaveRequestStatus.REJECTED, reason))
                .enqueue(callback);
    }
}
