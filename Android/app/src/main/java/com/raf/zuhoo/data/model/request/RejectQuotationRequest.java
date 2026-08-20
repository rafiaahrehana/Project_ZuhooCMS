package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class RejectQuotationRequest {

    @SerializedName("reason")
    private final String reason;

    public RejectQuotationRequest(String reason) {
        this.reason = reason;
    }
}
