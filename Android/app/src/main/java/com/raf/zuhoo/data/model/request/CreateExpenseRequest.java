package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class CreateExpenseRequest {

    @SerializedName("description")
    private final String description;
    @SerializedName("amount")
    private final BigDecimal amount;
    @SerializedName("category")
    private final String category;
    @SerializedName("vendorName")
    private final String vendorName;
    @SerializedName("expenseDate")
    private final String expenseDate;
    @SerializedName("receiptUrl")
    private final String receiptUrl;

    public CreateExpenseRequest(String description, BigDecimal amount, String category,
                                String vendorName, String expenseDate, String receiptUrl) {
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.vendorName = vendorName;
        this.expenseDate = expenseDate;
        this.receiptUrl = receiptUrl;
    }
}
