package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class ChangeRequestStatusRequest {

    @SerializedName("status")
    private final String status;
    @SerializedName("reason")
    private final String reason;

    public ChangeRequestStatusRequest(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }
}
