package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class InvoiceSummary {

    @SerializedName("id")
    private Long id;
    @SerializedName("invoiceNumber")
    private String invoiceNumber;
    @SerializedName("serviceRequestTitle")
    private String serviceRequestTitle;
    @SerializedName("status")
    private String status;
    @SerializedName("currency")
    private String currency;
    @SerializedName("items")
    private List<InvoiceItem> items;
    @SerializedName("subtotal")
    private BigDecimal subtotal;
    @SerializedName("taxAmount")
    private BigDecimal taxAmount;
    @SerializedName("totalAmount")
    private BigDecimal totalAmount;
    @SerializedName("paidAmount")
    private BigDecimal paidAmount;
    @SerializedName("balanceAmount")
    private BigDecimal balanceAmount;
    @SerializedName("invoiceDate")
    private String invoiceDate;
    @SerializedName("dueDate")
    private String dueDate;
    @SerializedName("notes")
    private String notes;

    public Long getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getServiceRequestTitle() {
        return serviceRequestTitle;
    }

    public String getStatus() {
        return status;
    }

    public String getCurrency() {
        return currency;
    }

    public List<InvoiceItem> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public String getInvoiceDate() {
        return invoiceDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getNotes() {
        return notes;
    }
}
