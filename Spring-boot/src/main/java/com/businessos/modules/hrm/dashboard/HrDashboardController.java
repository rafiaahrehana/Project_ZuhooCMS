package com.businessos.modules.hrm.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/dashboard")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class HrDashboardController {

    private final HrDashboardService hrDashboardService;

    /**
     * HR overview for the active company.
     *
     * The role gate above only narrows this to company users; the real check is
     * EMPLOYEE_VIEW inside the service, so an ordinary employee who happens to
     * hold the EMPLOYEE role cannot read company-wide payroll and headcount.
     */
    @GetMapping("/summary")
    public ResponseEntity<HrDashboardResponse> summary() {
        return ResponseEntity.ok(hrDashboardService.getSummary());
    }
}
