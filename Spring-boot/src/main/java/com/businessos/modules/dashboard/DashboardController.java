package com.businessos.modules.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(dashboardService.getSummary(from, to));
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/recommendations")
    public ResponseEntity<java.util.List<RecommendationResponse>> getRecommendations() {
        return ResponseEntity.ok(dashboardService.getRecommendations());
    }

    @PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
    @GetMapping("/insights")
    public ResponseEntity<InsightsResponse> getAiInsights() {
        return ResponseEntity.ok(dashboardService.getAiInsights());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'MARKETING_MANAGER', 'PLATFORM_ACCOUNTANT', 'SALES_MANAGER')")
    @GetMapping("/platform-summary")
    public ResponseEntity<PlatformSummaryResponse> getPlatformSummary() {
        return ResponseEntity.ok(dashboardService.getPlatformSummary());
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER', 'MARKETING_MANAGER', 'PLATFORM_ACCOUNTANT', 'SALES_MANAGER')")
    @GetMapping("/platform-metrics-history")
    public ResponseEntity<java.util.List<PlatformMetricsPoint>> getPlatformMetricsHistory(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(dashboardService.getPlatformMetricsHistory(Math.min(days, 365)));
    }

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/client-summary")
    public ResponseEntity<ClientSummaryResponse> getClientSummary() {
        return ResponseEntity.ok(dashboardService.getClientSummary());
    }
}
