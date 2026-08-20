package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class ServicePackageResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("effectivePrice")
    private BigDecimal effectivePrice;
    @SerializedName("billingCycle")
    private String billingCycle;
    @SerializedName("requestQuota")
    private Integer requestQuota;
    @SerializedName("featured")
    private boolean featured;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getEffectivePrice() {
        return effectivePrice;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public Integer getRequestQuota() {
        return requestQuota;
    }

    public boolean isFeatured() {
        return featured;
    }
}
