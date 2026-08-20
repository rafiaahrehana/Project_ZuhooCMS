package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class PayrollResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("payMonth")
    private int payMonth;
    @SerializedName("payYear")
    private int payYear;
    @SerializedName("netSalary")
    private BigDecimal netSalary;
    @SerializedName("status")
    private String status;
    @SerializedName("paidAt")
    private String paidAt;

    public Long getId() {
        return id;
    }

    public int getPayMonth() {
        return payMonth;
    }

    public int getPayYear() {
        return payYear;
    }

    public BigDecimal getNetSalary() {
        return netSalary;
    }

    public String getStatus() {
        return status;
    }

    public String getPaidAt() {
        return paidAt;
    }
}
