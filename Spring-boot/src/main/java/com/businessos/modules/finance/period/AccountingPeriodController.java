package com.businessos.modules.finance.period;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company/finance/accounting-periods")
@RequiredArgsConstructor
@Tag(name = "Accounting Periods", description = "Monthly period close / fiscal year close")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class AccountingPeriodController {

    private final AccountingPeriodService service;

    @GetMapping
    @Operation(summary = "List (generating if needed) the 12 periods for a fiscal year")
    public ResponseEntity<List<AccountingPeriodResponse>> listForYear(@RequestParam int fiscalYear) {
        return ResponseEntity.ok(service.listForYear(fiscalYear));
    }

    @GetMapping("/fiscal-years")
    @Operation(summary = "Rollup of every fiscal year that has periods - the Fiscal Years overview")
    public ResponseEntity<List<FiscalYearSummary>> listFiscalYears() {
        return ResponseEntity.ok(service.listFiscalYears());
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a period - blocks GL posting into it from then on")
    public ResponseEntity<AccountingPeriodResponse> closePeriod(@PathVariable Long id) {
        return ResponseEntity.ok(service.closePeriod(id));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen a closed period")
    public ResponseEntity<AccountingPeriodResponse> reopenPeriod(@PathVariable Long id) {
        return ResponseEntity.ok(service.reopenPeriod(id));
    }

    @PostMapping("/close-fiscal-year")
    @Operation(summary = "Close the fiscal year - requires all 12 periods closed; posts the year-end closing entry into Retained Earnings")
    public ResponseEntity<Void> closeFiscalYear(@RequestParam int fiscalYear) {
        service.closeFiscalYear(fiscalYear);
        return ResponseEntity.ok().build();
    }
}
