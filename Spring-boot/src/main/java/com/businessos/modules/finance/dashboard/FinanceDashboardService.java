package com.businessos.modules.finance.dashboard;

import com.businessos.enums.InvoiceStatus;
import com.businessos.modules.finance.budget.Budget;
import com.businessos.modules.finance.budget.BudgetRepository;
import com.businessos.modules.finance.expense.Expense;
import com.businessos.modules.finance.expense.ExpenseRepository;
import com.businessos.modules.finance.invoice.ClientInvoice;
import com.businessos.modules.finance.invoice.ClientInvoiceRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Aggregates the finance position for a month.
 *
 * Reads the source tables directly rather than the reporting endpoints: those
 * produce statements (P&L, balance sheet) for a period, whereas this needs a
 * handful of headline numbers plus a short rolling series, and going through
 * them would mean six calls to render one screen.
 */
@Service
@RequiredArgsConstructor
public class FinanceDashboardService {

    /** Invoices that represent real billed revenue - drafts and write-offs excluded. */
    private static final List<InvoiceStatus> BILLED = List.of(
            InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID,
            InvoiceStatus.PAID, InvoiceStatus.OVERDUE);

    private static final int TREND_MONTHS = 6;

    private final ClientInvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final SecurityUtil securityUtil;
    private final com.businessos.modules.finance.vendor.VendorBillRepository vendorBillRepository;
    private final com.businessos.modules.hrm.payroll.PayrollRepository payrollRepository;

    @Transactional(readOnly = true)
    public FinanceDashboardResponse build(int month, int year) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12.");
        }
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context for the current user");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        // One company-scoped, date-bounded read of each table for the whole trend
        // window, then bucketed in memory - cheaper than a query per month per
        // metric, and without dragging in other tenants' rows.
        LocalDate windowStart = ym.minusMonths(TREND_MONTHS - 1L).atDay(1);
        List<ClientInvoice> invoices =
                invoiceRepository.findByCompanyIdAndInvoiceDateBetween(companyId, windowStart, end);
        List<Expense> expenses =
                expenseRepository.findByCompanyIdAndExpenseDateBetween(companyId, windowStart, end);

        BigDecimal revenue = revenueIn(invoices, ym);
        BigDecimal spend = expensesIn(expenses, ym);
        YearMonth previous = ym.minusMonths(1);

        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        for (ClientInvoice i : invoices) {
            if (!inMonth(i.getInvoiceDate(), ym) || !BILLED.contains(i.getStatus())) continue;
            collected = collected.add(orZero(i.getPaidAmount()));
            BigDecimal balance = orZero(i.getBalanceAmount());
            outstanding = outstanding.add(balance);
            if (balance.compareTo(BigDecimal.ZERO) > 0
                    && i.getDueDate() != null && i.getDueDate().isBefore(today)) {
                overdue = overdue.add(balance);
            }
        }

        // Accounts payable: every vendor bill still carrying a balance.
        BigDecimal payables = BigDecimal.ZERO;
        BigDecimal payablesOverdue = BigDecimal.ZERO;
        for (var bill : vendorBillRepository.findByCompanyIdAndBalanceAmountGreaterThan(companyId, BigDecimal.ZERO)) {
            BigDecimal balance = orZero(bill.getBalanceAmount());
            payables = payables.add(balance);
            if (bill.getDueDate() != null && bill.getDueDate().isBefore(today)) {
                payablesOverdue = payablesOverdue.add(balance);
            }
        }

        // Payroll cost of the month: net across the period's payroll rows.
        BigDecimal payrollCost = payrollRepository
                .findAllByCompanyIdAndPayMonthAndPayYear(companyId, month, year).stream()
                .map(p -> orZero(p.getNetSalary()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FinanceDashboardResponse.builder()
                .payMonth(month).payYear(year)
                .totalRevenue(revenue)
                .totalExpenses(spend)
                .netProfit(revenue.subtract(spend))
                .cashCollected(collected)
                .outstanding(outstanding)
                .overdue(overdue)
                .payables(payables)
                .payablesOverdue(payablesOverdue)
                .payrollCost(payrollCost)
                .expenseByCategory(expenseByCategory(expenses, ym, spend))
                .revenueChangePercent(percentChange(revenueIn(invoices, previous), revenue))
                .expenseChangePercent(percentChange(expensesIn(expenses, previous), spend))
                .trend(trend(invoices, expenses, ym))
                .budgets(budgets(companyId, year, expenses))
                .recentInvoices(recentInvoices(invoices, today))
                .build();
    }

    private boolean inMonth(LocalDate date, YearMonth ym) {
        return date != null && YearMonth.from(date).equals(ym);
    }

    /** The month's approved spend grouped by category, largest first. */
    private List<FinanceDashboardResponse.CategorySlice> expenseByCategory(
            List<Expense> expenses, YearMonth ym, BigDecimal monthTotal) {
        java.util.Map<String, BigDecimal> byCategory = new java.util.TreeMap<>();
        for (Expense e : expenses) {
            if (!inMonth(e.getExpenseDate(), ym) || !isApprovedSpend(e)) continue;
            String category = e.getCategory() == null || e.getCategory().isBlank()
                    ? "UNCATEGORIZED" : e.getCategory();
            byCategory.merge(category, orZero(e.getAmount()), BigDecimal::add);
        }
        return byCategory.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(entry -> FinanceDashboardResponse.CategorySlice.builder()
                        .category(entry.getKey())
                        .amount(entry.getValue())
                        .percent(monthTotal.signum() > 0
                                ? entry.getValue().multiply(new BigDecimal("100"))
                                    .divide(monthTotal, 0, java.math.RoundingMode.HALF_UP).intValue()
                                : 0)
                        .build())
                .toList();
    }

    /**
     * Net of tax (subtotal - discount), matching exactly what postInvoiceToLedger
     * actually credits to Sales Revenue. Previously summed totalAmount
     * (tax-inclusive), so this tile disagreed with the P&L report's "Total
     * Revenue" for the identical month by the full tax rate on every invoice.
     */
    private BigDecimal revenueIn(List<ClientInvoice> invoices, YearMonth ym) {
        return invoices.stream()
                .filter(i -> inMonth(i.getInvoiceDate(), ym) && BILLED.contains(i.getStatus()))
                .map(i -> orZero(i.getSubtotal()).subtract(orZero(i.getDiscountAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Only expenses that have cleared approval count as spend. Pending claims
     * are not yet a cost, and counting them would overstate every month and
     * then silently correct itself when one is rejected.
     */
    private BigDecimal expensesIn(List<Expense> expenses, YearMonth ym) {
        return expenses.stream()
                .filter(e -> inMonth(e.getExpenseDate(), ym) && isApprovedSpend(e))
                .map(e -> orZero(e.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isApprovedSpend(Expense e) {
        if (e.getStatus() == null) return false;
        String s = e.getStatus().name();
        return "APPROVED".equals(s) || "PAID".equals(s) || "REIMBURSED".equals(s);
    }

    private List<FinanceDashboardResponse.MonthPoint> trend(List<ClientInvoice> invoices,
                                                            List<Expense> expenses, YearMonth current) {
        List<FinanceDashboardResponse.MonthPoint> points = new ArrayList<>();
        for (int back = TREND_MONTHS - 1; back >= 0; back--) {
            YearMonth ym = current.minusMonths(back);
            points.add(FinanceDashboardResponse.MonthPoint.builder()
                    .month(ym.getMonthValue())
                    .year(ym.getYear())
                    .label(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .revenue(revenueIn(invoices, ym))
                    .expenses(expensesIn(expenses, ym))
                    .build());
        }
        return points;
    }

    /**
     * Budget consumption for the fiscal year to date. Spend is matched to a
     * budget by category name, case-insensitively, which is how Budget and
     * Expense are already related - both hold the category as free text.
     */
    private List<FinanceDashboardResponse.BudgetLine> budgets(Long companyId, int year, List<Expense> expenses) {
        Map<String, BigDecimal> spentByCategory = new LinkedHashMap<>();
        for (Expense e : expenses) {
            if (!isApprovedSpend(e) || e.getCategory() == null) continue;
            if (e.getExpenseDate() == null || e.getExpenseDate().getYear() != year) continue;
            spentByCategory.merge(e.getCategory().toLowerCase(Locale.ROOT), orZero(e.getAmount()), BigDecimal::add);
        }

        List<FinanceDashboardResponse.BudgetLine> lines = new ArrayList<>();
        for (Budget b : budgetRepository.findByCompanyIdAndFiscalYearOrderByCategoryAsc(companyId, year)) {
            BigDecimal budgeted = orZero(b.getAmount());
            BigDecimal spent = spentByCategory.getOrDefault(
                    b.getCategory() == null ? "" : b.getCategory().toLowerCase(Locale.ROOT), BigDecimal.ZERO);
            int used = budgeted.compareTo(BigDecimal.ZERO) > 0
                    ? spent.multiply(BigDecimal.valueOf(100))
                        .divide(budgeted, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            lines.add(FinanceDashboardResponse.BudgetLine.builder()
                    .category(b.getCategory())
                    .budgeted(budgeted)
                    .spent(spent)
                    .usedPercent(used)
                    .build());
        }
        return lines;
    }

    private List<FinanceDashboardResponse.InvoiceLine> recentInvoices(List<ClientInvoice> invoices, LocalDate today) {
        return invoices.stream()
                .filter(i -> i.getStatus() != InvoiceStatus.DRAFT)
                .sorted(Comparator.comparing(ClientInvoice::getInvoiceDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(i -> FinanceDashboardResponse.InvoiceLine.builder()
                        .id(i.getId())
                        .invoiceNumber(i.getInvoiceNumber())
                        .clientName(i.getClient() != null ? i.getClient().getClientCompanyName() : null)
                        .dueDate(i.getDueDate())
                        .totalAmount(orZero(i.getTotalAmount()))
                        .balanceAmount(orZero(i.getBalanceAmount()))
                        .status(i.getStatus() == null ? null : i.getStatus().name())
                        .overdue(orZero(i.getBalanceAmount()).compareTo(BigDecimal.ZERO) > 0
                                && i.getDueDate() != null && i.getDueDate().isBefore(today))
                        .build())
                .toList();
    }

    /** Null rather than a made-up figure when the previous month was zero. */
    private BigDecimal percentChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
