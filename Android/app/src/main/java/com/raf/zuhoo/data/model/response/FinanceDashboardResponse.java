package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

// Trimmed to the top-line figures a quick mobile glance needs — the backend response also
// carries trend/budget/recent-invoice/category-breakdown lists meant for the web dashboard's
// charts, which this screen deliberately skips (Gson ignores the fields we don't declare).
public class FinanceDashboardResponse {

    @SerializedName("totalRevenue")
    private BigDecimal totalRevenue;
    @SerializedName("totalExpenses")
    private BigDecimal totalExpenses;
    @SerializedName("netProfit")
    private BigDecimal netProfit;
    @SerializedName("cashCollected")
    private BigDecimal cashCollected;
    @SerializedName("outstanding")
    private BigDecimal outstanding;
    @SerializedName("overdue")
    private BigDecimal overdue;

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public BigDecimal getNetProfit() {
        return netProfit;
    }

    public BigDecimal getCashCollected() {
        return cashCollected;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    public BigDecimal getOverdue() {
        return overdue;
    }
}
