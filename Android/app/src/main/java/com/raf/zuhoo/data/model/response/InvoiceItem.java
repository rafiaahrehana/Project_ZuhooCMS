package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class InvoiceItem {

    @SerializedName("description")
    private String description;
    @SerializedName("quantity")
    private BigDecimal quantity;
    @SerializedName("unitPrice")
    private BigDecimal unitPrice;
    @SerializedName("lineTotal")
    private BigDecimal lineTotal;

    public String getDescription() {
        return description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
