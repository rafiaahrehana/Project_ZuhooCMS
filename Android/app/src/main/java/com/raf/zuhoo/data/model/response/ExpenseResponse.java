package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class ExpenseResponse {

    @SerializedName("id")
    private Long id;
    @SerializedName("expenseNumber")
    private String expenseNumber;
    @SerializedName("description")
    private String description;
    @SerializedName("amount")
    private BigDecimal amount;
    @SerializedName("currency")
    private String currency;
    @SerializedName("vendorName")
    private String vendorName;
    @SerializedName("category")
    private String category;
    @SerializedName("expenseDate")
    private String expenseDate;
    @SerializedName("receiptUrl")
    private String receiptUrl;
    @SerializedName("status")
    private String status;
    @SerializedName("submittedByName")
    private String submittedByName;
    @SerializedName("approvedByName")
    private String approvedByName;
    @SerializedName("approvalNotes")
    private String approvalNotes;

    public Long getId() {
        return id;
    }

    public String getExpenseNumber() {
        return expenseNumber;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getCategory() {
        return category;
    }

    public String getExpenseDate() {
        return expenseDate;
    }

    public String getReceiptUrl() {
        return receiptUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getSubmittedByName() {
        return submittedByName;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public String getApprovalNotes() {
        return approvalNotes;
    }
}
