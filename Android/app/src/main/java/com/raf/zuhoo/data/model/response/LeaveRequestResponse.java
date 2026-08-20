package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class LeaveRequestResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("leaveType")
    private String leaveType;
    @SerializedName("startDate")
    private String startDate;
    @SerializedName("endDate")
    private String endDate;
    @SerializedName("totalDays")
    private int totalDays;
    @SerializedName("reason")
    private String reason;
    @SerializedName("status")
    private String status;
    @SerializedName("rejectionReason")
    private String rejectionReason;
    @SerializedName("employeeId")
    private Long employeeId;
    @SerializedName("employeeName")
    private String employeeName;

    public Long getId() {
        return id;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public int getTotalDays() {
        return totalDays;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }
}
