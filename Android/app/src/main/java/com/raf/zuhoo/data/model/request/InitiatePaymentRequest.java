package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class InitiatePaymentRequest {

    @SerializedName("purpose")
    private final String purpose;
    @SerializedName("targetId")
    private final Long targetId;
    @SerializedName("amount")
    private final BigDecimal amount;

    public InitiatePaymentRequest(String purpose, Long targetId, BigDecimal amount) {
        this.purpose = purpose;
        this.targetId = targetId;
        this.amount = amount;
    }
}
