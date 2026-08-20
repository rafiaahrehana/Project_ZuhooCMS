package com.businessos.modules.finance.reports.controller;

import com.businessos.modules.finance.reports.dto.AccountLedger;
import com.businessos.modules.finance.reports.dto.AgeingReport;
import com.businessos.modules.finance.reports.dto.BalanceSheetReport;
import com.businessos.modules.finance.reports.dto.CashFlowReport;
import com.businessos.modules.finance.reports.dto.ProfitLossReport;
import com.businessos.modules.finance.reports.dto.TrialBalanceReport;
import com.businessos.modules.finance.reports.service.FinancialReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/company/finance/reports")
@RequiredArgsConstructor
@Tag(name = "Financial Reports", description = "Financial Reports Management")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class FinancialReportController {

    private final FinancialReportService service;

    @GetMapping("/profit-loss")
    @Operation(summary = "Generate Profit and Loss Report")
    public ResponseEntity<ProfitLossReport> generateProfitLossReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.generateProfitLossReport(startDate, endDate));
    }

    @GetMapping("/balance-sheet")
    @Operation(summary = "Generate Balance Sheet Report")
    public ResponseEntity<BalanceSheetReport> generateBalanceSheetReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(service.generateBalanceSheetReport(asOfDate));
    }

    @GetMapping("/trial-balance")
    @Operation(summary = "Generate Trial Balance Report")
    public ResponseEntity<TrialBalanceReport> generateTrialBalanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(service.generateTrialBalanceReport(asOfDate));
    }

    @GetMapping("/ledger")
    @Operation(summary = "Generate Account Ledger Report")
    public ResponseEntity<AccountLedger> generateAccountLedger(
            @RequestParam Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.generateAccountLedger(accountId, startDate, endDate));
    }

    @GetMapping("/ageing")
    @Operation(summary = "Generate Accounts Receivable Ageing Report")
    public ResponseEntity<AgeingReport> generateAgeingReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        return ResponseEntity.ok(service.generateAgeingReport(asOfDate));
    }

    @GetMapping("/cash-flow")
    @Operation(summary = "Generate Cash Flow Statement")
    public ResponseEntity<CashFlowReport> generateCashFlowReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(service.generateCashFlowReport(startDate, endDate));
    }
}
