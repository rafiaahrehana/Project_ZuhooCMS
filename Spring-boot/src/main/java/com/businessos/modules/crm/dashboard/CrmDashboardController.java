package com.businessos.modules.crm.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crm/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_OWNER','EMPLOYEE')")
public class CrmDashboardController {

    private final CrmDashboardService crmDashboardService;

    @GetMapping("/summary")
    public CrmDashboardSummaryResponse summary() {
        return crmDashboardService.getSummary();
    }
}
