package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class AttendanceResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("attendanceDate")
    private String attendanceDate;
    @SerializedName("checkInTime")
    private String checkInTime;
    @SerializedName("checkOutTime")
    private String checkOutTime;
    @SerializedName("status")
    private String status;
    @SerializedName("checkInSelfieUrl")
    private String checkInSelfieUrl;
    @SerializedName("locationFlagged")
    private boolean locationFlagged;
    @SerializedName("locationFlagReason")
    private String locationFlagReason;

    public Long getId() {
        return id;
    }

    public String getAttendanceDate() {
        return attendanceDate;
    }

    public String getCheckInTime() {
        return checkInTime;
    }

    public String getCheckOutTime() {
        return checkOutTime;
    }

    public String getStatus() {
        return status;
    }

    public String getCheckInSelfieUrl() {
        return checkInSelfieUrl;
    }

    public boolean isLocationFlagged() {
        return locationFlagged;
    }

    public String getLocationFlagReason() {
        return locationFlagReason;
    }

    public boolean isCheckedIn() {
        return checkInTime != null;
    }

    public boolean isCheckedOut() {
        return checkOutTime != null;
    }
}
