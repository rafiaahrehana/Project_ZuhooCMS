package com.businessos.modules.finance.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Finance dashboard for one month, aggregated live from invoices, expenses and
 * budgets. Nothing here is stored - it is a read of the current position.
 */
@Data
@Builder
public class FinanceDashboardResponse {

    private int payMonth;
    private int payYear;

    // ── Headline figures for the month ───────────────────────
    /** Invoiced total, excluding drafts and cancelled invoices. */
    private BigDecimal totalRevenue;
    /** Approved and reimbursed expenses. */
    private BigDecimal totalExpenses;
    /** Revenue less expenses. */
    private BigDecimal netProfit;
    /** Cash actually collected: the paid portion of invoices. */
    private BigDecimal cashCollected;
    /** Invoiced but not yet collected. */
    private BigDecimal outstanding;
    /** Outstanding on invoices already past their due date. */
    private BigDecimal overdue;

    /** Percentage change against the previous month, null when there is no base. */
    private BigDecimal revenueChangePercent;
    private BigDecimal expenseChangePercent;

    // Accounts payable: open vendor-bill balances, and how much is overdue.
    private BigDecimal payables;
    private BigDecimal payablesOverdue;

    /** This month's payroll cost (net paid/payable across the period's rows). */
    private BigDecimal payrollCost;

    private List<MonthPoint> trend;
    private List<BudgetLine> budgets;
    private List<InvoiceLine> recentInvoices;
    private List<CategorySlice> expenseByCategory;

    /** One slice of the month's approved spend, grouped by category. */
    @Data
    @Builder
    public static class CategorySlice {
        private String category;
        private BigDecimal amount;
        /** Share of the month's spend, rounded to a whole percent. */
        private int percent;
    }

    /** One month of the rolling revenue-vs-expenses series. */
    @Data
    @Builder
    public static class MonthPoint {
        private int month;
        private int year;
        private String label;
        private BigDecimal revenue;
        private BigDecimal expenses;
    }

    /** Spend against budget for one category in the current fiscal year. */
    @Data
    @Builder
    public static class BudgetLine {
        private String category;
        private BigDecimal budgeted;
        private BigDecimal spent;
        /** Rounded to a whole number; can exceed 100 when overspent. */
        private int usedPercent;
    }

    @Data
    @Builder
    public static class InvoiceLine {
        private Long id;
        private String invoiceNumber;
        private String clientName;
        private LocalDate dueDate;
        private BigDecimal totalAmount;
        private BigDecimal balanceAmount;
        private String status;
        private boolean overdue;
    }
}
