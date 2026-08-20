package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class CreateLeaveRequestRequest {

    @SerializedName("leaveType")
    private final String leaveType;
    @SerializedName("startDate")
    private final String startDate;
    @SerializedName("endDate")
    private final String endDate;
    @SerializedName("reason")
    private final String reason;

    public CreateLeaveRequestRequest(String leaveType, String startDate, String endDate, String reason) {
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }
}
