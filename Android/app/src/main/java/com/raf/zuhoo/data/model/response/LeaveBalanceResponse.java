package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class LeaveBalanceResponse {

    @SerializedName("leaveType")
    private String leaveType;
    @SerializedName("entitledDays")
    private int entitledDays;
    @SerializedName("usedDays")
    private int usedDays;
    @SerializedName("pendingDays")
    private int pendingDays;
    @SerializedName("remainingDays")
    private int remainingDays;

    public String getLeaveType() {
        return leaveType;
    }

    public int getEntitledDays() {
        return entitledDays;
    }

    public int getUsedDays() {
        return usedDays;
    }

    public int getPendingDays() {
        return pendingDays;
    }

    public int getRemainingDays() {
        return remainingDays;
    }
}
