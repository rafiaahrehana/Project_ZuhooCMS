package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class SubmitQuotationRequest {

    @SerializedName("amount")
    private final BigDecimal amount;
    @SerializedName("currency")
    private final String currency;
    @SerializedName("notes")
    private final String notes;

    public SubmitQuotationRequest(BigDecimal amount, String currency, String notes) {
        this.amount = amount;
        this.currency = currency;
        this.notes = notes;
    }
}
