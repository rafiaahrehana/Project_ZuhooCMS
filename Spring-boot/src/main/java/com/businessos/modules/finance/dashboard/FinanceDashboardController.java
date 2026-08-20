package com.businessos.modules.finance.dashboard;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Gated on FINANCIAL_REPORT_VIEW rather than INVOICE_VIEW: this aggregates
 * revenue, spend and margin for the whole company, which is strictly more
 * revealing than any single entity list.
 */
@RestController
@RequestMapping("/api/finance/dashboard")
@RequiredArgsConstructor
public class FinanceDashboardController {

    private final FinanceDashboardService service;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<FinanceDashboardResponse> get(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        authorizationService.checkPermission(PermissionCode.FINANCIAL_REPORT_VIEW);

        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(service.build(
                month != null ? month : now.getMonthValue(),
                year != null ? year : now.getYear()));
    }
}
