package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class WalletResponse {

    @SerializedName("balance")
    private BigDecimal balance;
    @SerializedName("creditBalance")
    private BigDecimal creditBalance;
    @SerializedName("totalAvailable")
    private BigDecimal totalAvailable;
    @SerializedName("currency")
    private String currency;

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public BigDecimal getTotalAvailable() {
        return totalAvailable;
    }

    public String getCurrency() {
        return currency;
    }
}
