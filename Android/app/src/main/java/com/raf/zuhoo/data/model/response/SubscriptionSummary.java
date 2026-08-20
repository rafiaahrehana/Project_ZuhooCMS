package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class SubscriptionSummary {

    @SerializedName("id")
    private Long id;
    @SerializedName("packageId")
    private Long packageId;
    @SerializedName("packageName")
    private String packageName;
    @SerializedName("status")
    private String status;
    @SerializedName("billingCycle")
    private String billingCycle;
    @SerializedName("pricePaid")
    private BigDecimal pricePaid;
    @SerializedName("requestQuota")
    private Integer requestQuota;
    @SerializedName("requestsUsed")
    private int requestsUsed;
    @SerializedName("remainingRequests")
    private int remainingRequests;
    @SerializedName("autoRenew")
    private boolean autoRenew;
    @SerializedName("nextBillingDate")
    private String nextBillingDate;
    @SerializedName("endDate")
    private String endDate;

    public Long getId() {
        return id;
    }

    public Long getPackageId() {
        return packageId;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getStatus() {
        return status;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public BigDecimal getPricePaid() {
        return pricePaid;
    }

    public Integer getRequestQuota() {
        return requestQuota;
    }

    public int getRequestsUsed() {
        return requestsUsed;
    }

    public int getRemainingRequests() {
        return remainingRequests;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public String getNextBillingDate() {
        return nextBillingDate;
    }

    public String getEndDate() {
        return endDate;
    }
}
