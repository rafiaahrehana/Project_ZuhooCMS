package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class ServiceRequestDetail {

    @SerializedName("id")
    private Long id;
    @SerializedName("title")
    private String title;
    @SerializedName("description")
    private String description;
    @SerializedName("status")
    private String status;
    @SerializedName("priority")
    private String priority;
    @SerializedName("agreedPrice")
    private BigDecimal agreedPrice;
    @SerializedName("hubServiceId")
    private Long hubServiceId;
    @SerializedName("hubServiceName")
    private String hubServiceName;
    @SerializedName("assignedEmployeeName")
    private String assignedEmployeeName;
    @SerializedName("quotationAmount")
    private BigDecimal quotationAmount;
    @SerializedName("quotationCurrency")
    private String quotationCurrency;
    @SerializedName("quotationNotes")
    private String quotationNotes;
    @SerializedName("quotationStatus")
    private String quotationStatus;
    @SerializedName("invoiceId")
    private Long invoiceId;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("updatedAt")
    private String updatedAt;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getPriority() {
        return priority;
    }

    public BigDecimal getAgreedPrice() {
        return agreedPrice;
    }

    public Long getHubServiceId() {
        return hubServiceId;
    }

    public String getHubServiceName() {
        return hubServiceName;
    }

    public String getAssignedEmployeeName() {
        return assignedEmployeeName;
    }

    public BigDecimal getQuotationAmount() {
        return quotationAmount;
    }

    public String getQuotationCurrency() {
        return quotationCurrency;
    }

    public String getQuotationNotes() {
        return quotationNotes;
    }

    public String getQuotationStatus() {
        return quotationStatus;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
