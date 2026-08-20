package com.raf.zuhoo.data.repository;

import android.content.Context;

import com.raf.zuhoo.data.api.ApiClient;
import com.raf.zuhoo.data.api.ApiService;
import com.raf.zuhoo.data.model.request.AttendanceCheckInRequest;
import com.raf.zuhoo.data.model.request.AttendanceCheckOutRequest;
import com.raf.zuhoo.data.model.request.UpdateAttendanceLocationSettingsRequest;
import com.raf.zuhoo.data.model.response.AttendanceLocationSettingsResponse;
import com.raf.zuhoo.data.model.response.AttendanceResponse;

import retrofit2.Callback;

public class AttendanceRepository {

    private final ApiService apiService;

    public AttendanceRepository(Context context) {
        apiService = ApiClient.getClient(context);
    }

    public void getMyTodayAttendance(Callback<AttendanceResponse> callback) {
        apiService.getMyTodayAttendance().enqueue(callback);
    }

    public void checkIn(String latitude, String longitude, String selfieUrl,
                        Callback<AttendanceResponse> callback) {
        apiService.checkInAttendance(new AttendanceCheckInRequest(latitude, longitude, selfieUrl))
                .enqueue(callback);
    }

    public void checkOut(Long attendanceId, String latitude, String longitude, String selfieUrl,
                         Callback<AttendanceResponse> callback) {
        apiService.checkOutAttendance(attendanceId,
                new AttendanceCheckOutRequest(latitude, longitude, selfieUrl)).enqueue(callback);
    }

    public void getLocationSettings(Callback<AttendanceLocationSettingsResponse> callback) {
        apiService.getAttendanceLocationSettings().enqueue(callback);
    }

    public void updateLocationSettings(double latitude, double longitude, int radiusMeters,
                                       boolean enforcementEnabled,
                                       Callback<AttendanceLocationSettingsResponse> callback) {
        apiService.updateAttendanceLocationSettings(new UpdateAttendanceLocationSettingsRequest(
                latitude, longitude, radiusMeters, enforcementEnabled)).enqueue(callback);
    }
}
