package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class WalletTransactionResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("type")
    private String type;
    @SerializedName("amount")
    private BigDecimal amount;
    @SerializedName("balanceAfter")
    private BigDecimal balanceAfter;
    @SerializedName("reference")
    private String reference;
    @SerializedName("notes")
    private String notes;
    @SerializedName("transactedAt")
    private String transactedAt;

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getReference() {
        return reference;
    }

    public String getNotes() {
        return notes;
    }

    public String getTransactedAt() {
        return transactedAt;
    }
}
