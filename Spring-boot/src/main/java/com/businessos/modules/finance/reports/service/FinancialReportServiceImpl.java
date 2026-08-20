package com.businessos.modules.finance.reports.service;

import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccountRepository;
import com.businessos.modules.finance.chartofaccounts.AccountType;
import com.businessos.modules.finance.chartofaccounts.DefaultAccountResolver;
import com.businessos.modules.finance.generalledger.GeneralLedger;
import com.businessos.modules.finance.generalledger.GeneralLedgerMapper;
import com.businessos.modules.finance.generalledger.GeneralLedgerRepository;
import com.businessos.modules.finance.generalledger.GeneralLedgerResponse;
import com.businessos.modules.finance.invoice.ClientInvoice;
import com.businessos.modules.finance.invoice.ClientInvoiceRepository;
import com.businessos.modules.finance.reports.dto.AccountLedger;
import com.businessos.modules.finance.reports.dto.AgeingReport;
import com.businessos.modules.finance.reports.dto.BalanceSheetReport;
import com.businessos.modules.finance.reports.dto.CashFlowReport;
import com.businessos.modules.finance.reports.dto.ProfitLossReport;
import com.businessos.modules.finance.reports.dto.TrialBalanceReport;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.enums.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private final ChartOfAccountRepository coaRepository;
    private final GeneralLedgerRepository glRepository;
    private final ClientInvoiceRepository invoiceRepository;
    private final DefaultAccountResolver accountResolver;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional(readOnly = true)
    public ProfitLossReport generateProfitLossReport(LocalDate startDate, LocalDate endDate) {
        authorizationService.checkPermission(PermissionCode.FINANCIAL_REPORT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();

        // Revenue/Expense are period ("flow") figures - sum ledger movement within the
        // requested window, not the account's live all-time balance.
        List<GeneralLedger> transactions = glRepository.findTransactionsBetweenDates(companyId, startDate, endDate);

        BigDecimal grossRevenue = transactions.stream()
                .filter(gl -> gl.getAccount().getType() == AccountType.REVENUE)
                .map(gl -> nz(gl.getCreditAmount()).subtract(nz(gl.getDebitAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Contra-revenue (returns, discounts, allowances) is debit-normal and reduces
        // revenue - previously excluded entirely, which overstated revenue by however
        // much had been recorded against a contra-revenue account.
        BigDecimal contraRevenue = transactions.stream()
                .filter(gl -> gl.getAccount().getType() == AccountType.CONTRA_REVENUE)
                .map(gl -> nz(gl.getDebitAmount()).subtract(nz(gl.getCreditAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = grossRevenue.subtract(contraRevenue);

        BigDecimal totalExpense = transactions.stream()
                .filter(gl -> gl.getAccount().getType() == AccountType.EXPENSE)
                .map(gl -> nz(gl.getDebitAmount()).subtract(nz(gl.getCreditAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        return ProfitLossReport.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .totalRevenue(totalRevenue)
                .totalExpense(totalExpense)
                .netProfit(netProfit)
                .generatedDate(LocalDate.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceSheetReport generateBalanceSheetReport(LocalDate asOfDate) {
        authorizationService.checkPermission(PermissionCode.FINANCIAL_REPORT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();

        // Assets/Liabilities/Equity are point-in-time balances - rebuild each from the
        // ledger up to asOfDate rather than trusting the account's *current* live balance,
        // which would be wrong for any asOfDate other than today.
        // Contra-asset (e.g. Accumulated Depreciation) and contra-liability accounts were
        // previously never queried at all, overstating assets/liabilities by whatever had
        // been recorded against them - net them out here.
        BigDecimal totalAssets = balanceAsOf(companyId, AccountType.ASSET, asOfDate)
                .subtract(balanceAsOf(companyId, AccountType.CONTRA_ASSET, asOfDate));
        BigDecimal totalLiabilities = balanceAsOf(companyId, AccountType.LIABILITY, asOfDate)
                .subtract(balanceAsOf(companyId, AccountType.CONTRA_LIABILITY, asOfDate));
        BigDecimal totalEquity = balanceAsOf(companyId, AccountType.EQUITY, asOfDate);

        // Assets = Liabilities + Equity is the fundamental accounting identity - if this
        // doesn't hold, something posted an unbalanced entry (or period-end closing hasn't
        // run), and the report should say so loudly rather than silently show wrong numbers.
        BigDecimal outOfBalanceAmount = totalAssets.subtract(totalLiabilities.add(totalEquity));
        boolean balanced = outOfBalanceAmount.abs().compareTo(new BigDecimal("0.01")) <= 0;

        return BalanceSheetReport.builder()
                .asOfDate(asOfDate)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .totalEquity(totalEquity)
                .balanced(balanced)
                .outOfBalanceAmount(outOfBalanceAmount)
                .build();
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal balanceAsOf(Long companyId, AccountType type, LocalDate asOfDate) {
        List<ChartOfAccount> accounts = coaRepository.findByCompanyIdAndType(companyId, type);
        if (accounts.isEmpty()) return BigDecimal.ZERO;

        List<Long> accountIds = accounts.stream().map(ChartOfAccount::getId).collect(Collectors.toList());
        List<GeneralLedger> transactions = glRepository.findByCompanyIdAndAccountIdsUpToDate(companyId, accountIds, asOfDate);
        return sumSigned(transactions, type.isCreditNormal());
    }

    private BigDecimal accountBalanceAsOf(Long companyId, Long accountId, boolean creditNormal, LocalDate asOfDate) {
        List<GeneralLedger> transactions = glRepository.findByCompanyIdAndAccountIdsUpToDate(
                companyId, List.of(accountId), asOfDate);
        return sumSigned(transactions, creditNormal);
    }

    private static BigDecimal sumSigned(List<GeneralLedger> transactions, boolean creditNormal) {
        return transactions.stream()
                .map(gl -> creditNormal
                        ? nz(gl.getCreditAmount()).subtract(nz(gl.getDebitAmount()))
                        : nz(gl.getDebitAmount()).subtract(nz(gl.getCreditAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public TrialBalanceReport generateTrialBalanceReport(LocalDate asOfDate) {
        authorizationService.checkPermission(PermissionCode.FINANCIAL_REPORT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();

        List<ChartOfAccount> accounts = coaRepository.findByCompanyIdAndActive(companyId, true);

        // Previously read ChartOfAccount.balance (the account's *current* live balance)
        // split by normal side - asOfDate was accepted but silently ignored, so a Trial
        // Balance "as of" any past date returned today's numbers. Replay the ledger up to
        // asOfDate per account instead, same as Balance Sheet already does.
        List<TrialBalanceReport.AccountBalance> balances = accounts.stream()
                .map(acc -> {
                    boolean creditNormal = acc.getType().isCreditNormal();
                    // Positive = sitting on its normal side; negative means this particular
                    // account is abnormally balanced (e.g. an overdrawn bank account) - it
                    // still has to land in *some* column, just the opposite one, or
                    // totalDebit would stop equalling totalCredit for no real reason.
                    BigDecimal balance = accountBalanceAsOf(companyId, acc.getId(), creditNormal, asOfDate);
                    boolean normalSide = balance.signum() >= 0;
                    BigDecimal amount = balance.abs();
                    boolean showsAsDebit = creditNormal != normalSide; // XOR: flips to the other column when abnormal
                    return TrialBalanceReport.AccountBalance.builder()
                            .accountId(acc.getId())
                            .accountCode(acc.getAccountCode())
                            .accountName(acc.getAccountName())
                            .debitBalance(showsAsDebit ? amount : BigDecimal.ZERO)
                            .creditBalance(showsAsDebit ? BigDecimal.ZERO : amount)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal totalDebit = balances.stream()
                .map(TrialBalanceReport.AccountBalance::getDebitBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredit = balances.stream()
                .map(TrialBalanceReport.AccountBalance::getCreditBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return TrialBalanceReport.builder()
                .asOfDate(asOfDate)
                .accounts(balances)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .generatedDate(LocalDate.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AgeingReport generateAgeingReport(LocalDate asOfDate) {
        authorizationService.checkPermission(PermissionCode.FINANCIAL_REPORT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        List<InvoiceStatus> outstandingStatuses = List.of(
                InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID, InvoiceStatus.OVERDUE);
        List<ClientInvoice> outstanding = invoiceRepository.findOutstandingByCompanyId(companyId, outstandingStatuses);

        BigDecimal current = BigDecimal.ZERO, d1to30 = BigDecimal.ZERO, d31to60 = BigDecimal.ZERO,
                d61to90 = BigDecimal.ZERO, over90 = BigDecimal.ZERO;
        List<AgeingReport.AgeingLine> lines = new java.util.ArrayList<>();

        for (ClientInvoice invoice : outstanding) {
            long daysOverdue = invoice.getDueDate() != null
                    ? java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), asOfDate) : 0;
            BigDecimal balance = invoice.getBalanceAmount();
            String bucket;
            if (daysOverdue <= 0) { bucket = "CURRENT"; current = current.add(balance); }
            else if (daysOverdue <= 30) { bucket = "1-30"; d1to30 = d1to30.add(balance); }
            else if (daysOverdue <= 60) { bucket = "31-60"; d31to60 = d31to60.add(balance); }
            else if (daysOverdue <= 90) { bucket = "61-90"; d61to90 = d61to90.add(balance); }
            else { bucket = "90+"; over90 = over90.add(balance); }

            lines.add(AgeingReport.AgeingLine.builder()
                    .invoiceId(invoice.getId())
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .clientId(invoice.getClient() != null ? invoice.getClient().getId() : null)
                    .clientName(invoice.getClient() != null ? invoice.getClient().getClientCompanyName() : null)
                    .dueDate(invoice.getDueDate())
                    .balanceAmount(balance)
                    .daysOverdue(daysOverdue)
                    .bucket(bucket)
                    .build());
        }

        BigDecimal total = current.add(d1to30).add(d31to60).add(d61to90).add(over90);

        return AgeingReport.builder()
                .asOfDate(asOfDate)
                .current(current)
                .days1to30(d1to30)
                .days31to60(d31to60)
                .days61to90(d61to90)
                .over90(over90)
                .totalOutstanding(total)
                .lines(lines)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CashFlowReport generateCashFlowReport(LocalDate startDate, LocalDate endDate) {
        authorizationService.checkPermission(PermissionCode.FINANCIAL_REPORT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        ChartOfAccount cash = accountResolver.cash(companyId);

        // Cash is debit-normal (ASSET) - same opening/closing logic as generateAccountLedger.
        List<GeneralLedger> priorTransactions = glRepository
                .findByCompanyIdAndAccountIdBeforeDate(companyId, cash.getId(), startDate);
        BigDecimal openingBalance = priorTransactions.stream()
                .map(gl -> nz(gl.getDebitAmount()).subtract(nz(gl.getCreditAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<GeneralLedger> periodEntries = glRepository
                .findTransactionsBetweenDates(companyId, startDate, endDate)
                .stream()
                .filter(gl -> gl.getAccount().getId().equals(cash.getId()))
                .collect(Collectors.toList());

        Map<String, BigDecimal[]> byCategory = new LinkedHashMap<>(); // [inflow, outflow]
        BigDecimal totalInflows = BigDecimal.ZERO, totalOutflows = BigDecimal.ZERO;

        for (GeneralLedger gl : periodEntries) {
            String category = categoryFor(gl.getReferenceType());
            BigDecimal inflow = nz(gl.getDebitAmount());
            BigDecimal outflow = nz(gl.getCreditAmount());
            totalInflows = totalInflows.add(inflow);
            totalOutflows = totalOutflows.add(outflow);

            BigDecimal[] bucket = byCategory.computeIfAbsent(category, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            bucket[0] = bucket[0].add(inflow);
            bucket[1] = bucket[1].add(outflow);
        }

        List<CashFlowReport.CashFlowLine> lines = byCategory.entrySet().stream()
                .map(e -> CashFlowReport.CashFlowLine.builder()
                        .category(e.getKey())
                        .inflow(e.getValue()[0])
                        .outflow(e.getValue()[1])
                        .build())
                .collect(Collectors.toList());

        BigDecimal netChange = totalInflows.subtract(totalOutflows);

        return CashFlowReport.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .openingBalance(openingBalance)
                .closingBalance(openingBalance.add(netChange))
                .totalInflows(totalInflows)
                .totalOutflows(totalOutflows)
                .netChange(netChange)
                .lines(lines)
                .build();
    }

    private static String categoryFor(String referenceType) {
        if (referenceType == null) return "Other";
        return switch (referenceType) {
            case "INVOICE" -> "Customer Payments";
            case "PAYMENT_RECEIPT" -> "Customer Payments";
            case "EXPENSE" -> "Expenses Paid";
            case "PAYROLL" -> "Payroll";
            case "INVOICE_CANCEL" -> "Invoice Cancellations";
            case "JOURNAL_ENTRY" -> "Manual Journal Entries";
            default -> "Other";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public AccountLedger generateAccountLedger(Long accountId, LocalDate startDate, LocalDate endDate) {
        authorizationService.checkPermission(PermissionCode.FINANCIAL_REPORT_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();

        ChartOfAccount account = coaRepository.findByIdAndCompanyId(accountId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        boolean creditNormal = account.getType().isCreditNormal();

        // Opening balance = everything before the period start; closing = opening plus
        // this period's movement. Previously both were the account's current live balance,
        // so a report for last month looked identical to one for last year.
        List<GeneralLedger> priorTransactions = glRepository.findByCompanyIdAndAccountIdBeforeDate(companyId, accountId, startDate);
        BigDecimal openingBalance = priorTransactions.stream()
                .map(gl -> creditNormal
                        ? nz(gl.getCreditAmount()).subtract(nz(gl.getDebitAmount()))
                        : nz(gl.getDebitAmount()).subtract(nz(gl.getCreditAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<GeneralLedger> periodEntries = glRepository
                .findTransactionsBetweenDates(companyId, startDate, endDate)
                .stream()
                .filter(gl -> gl.getAccount().getId().equals(accountId))
                .collect(Collectors.toList());

        BigDecimal periodMovement = periodEntries.stream()
                .map(gl -> creditNormal
                        ? nz(gl.getCreditAmount()).subtract(nz(gl.getDebitAmount()))
                        : nz(gl.getDebitAmount()).subtract(nz(gl.getCreditAmount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<GeneralLedgerResponse> entries = periodEntries.stream()
                .map(GeneralLedgerMapper::toResponse)
                .collect(Collectors.toList());

        return AccountLedger.builder()
                .accountId(account.getId())
                .accountCode(account.getAccountCode())
                .accountName(account.getAccountName())
                .periodStart(startDate)
                .periodEnd(endDate)
                .openingBalance(openingBalance)
                .entries(entries)
                .closingBalance(openingBalance.add(periodMovement))
                .generatedDate(LocalDate.now())
                .build();
    }
}


