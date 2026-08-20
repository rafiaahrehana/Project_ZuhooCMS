package com.businessos.modules.finance.reports.service;

import com.businessos.modules.finance.reports.dto.AccountLedger;
import com.businessos.modules.finance.reports.dto.AgeingReport;
import com.businessos.modules.finance.reports.dto.BalanceSheetReport;
import com.businessos.modules.finance.reports.dto.CashFlowReport;
import com.businessos.modules.finance.reports.dto.ProfitLossReport;
import com.businessos.modules.finance.reports.dto.TrialBalanceReport;

import java.time.LocalDate;

public interface FinancialReportService {
    ProfitLossReport generateProfitLossReport(LocalDate startDate, LocalDate endDate);
    BalanceSheetReport generateBalanceSheetReport(LocalDate asOfDate);
    TrialBalanceReport generateTrialBalanceReport(LocalDate asOfDate);
    AccountLedger generateAccountLedger(Long accountId, LocalDate startDate, LocalDate endDate);
    AgeingReport generateAgeingReport(LocalDate asOfDate);
    CashFlowReport generateCashFlowReport(LocalDate startDate, LocalDate endDate);
}
