package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class ReviewLeaveRequestRequest {

    @SerializedName("status")
    private final String status;
    @SerializedName("rejectionReason")
    private final String rejectionReason;

    public ReviewLeaveRequestRequest(String status, String rejectionReason) {
        this.status = status;
        this.rejectionReason = rejectionReason;
    }
}
