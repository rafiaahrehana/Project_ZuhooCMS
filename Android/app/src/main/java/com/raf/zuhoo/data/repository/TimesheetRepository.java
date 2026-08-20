package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.LogTimesheetRequest;
import com.raf.zuhoo.data.model.response.PageResponse;
import com.raf.zuhoo.data.model.response.TimesheetResponse;

import retrofit2.Callback;

public class TimesheetRepository {

    private final ApiService apiService;

    public TimesheetRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMyTimesheets(Callback<PageResponse<TimesheetResponse>> callback) {
        apiService.getMyTimesheets().enqueue(callback);
    }

    public void logTimesheet(String workDate, double hoursWorked, String projectName, String description,
                             Callback<TimesheetResponse> callback) {
        apiService.logTimesheet(new LogTimesheetRequest(workDate, hoursWorked, projectName, description))
                .enqueue(callback);
    }
}
