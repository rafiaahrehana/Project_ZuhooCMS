package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

// Trimmed to what a mobile glance needs — skips activity history, tags, duplicate-match, and the
// AI-summary field (only populated by a separate /summary endpoint this screen doesn't call).
public class LeadResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("contactName")
    private String contactName;
    @SerializedName("companyName")
    private String companyName;
    @SerializedName("email")
    private String email;
    @SerializedName("phone")
    private String phone;
    @SerializedName("industry")
    private String industry;
    @SerializedName("notes")
    private String notes;
    @SerializedName("status")
    private String status;
    @SerializedName("priority")
    private String priority;
    @SerializedName("estimatedValue")
    private BigDecimal estimatedValue;
    @SerializedName("assignedToName")
    private String assignedToName;

    public Long getId() {
        return id;
    }

    public String getContactName() {
        return contactName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getIndustry() {
        return industry;
    }

    public String getNotes() {
        return notes;
    }

    public String getStatus() {
        return status;
    }

    public String getPriority() {
        return priority;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public String getAssignedToName() {
        return assignedToName;
    }
}
