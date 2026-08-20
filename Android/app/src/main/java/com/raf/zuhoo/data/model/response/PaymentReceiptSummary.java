package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class PaymentReceiptSummary {

    @SerializedName("id")
    private Long id;
    @SerializedName("receiptNumber")
    private String receiptNumber;
    @SerializedName("invoiceNumber")
    private String invoiceNumber;
    @SerializedName("amount")
    private BigDecimal amount;
    @SerializedName("paymentDate")
    private String paymentDate;
    @SerializedName("paymentMethod")
    private String paymentMethod;
    @SerializedName("status")
    private String status;

    public Long getId() {
        return id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public String getStatus() {
        return status;
    }
}
