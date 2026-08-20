package com.businessos.core.scheduler;

import com.businessos.modules.servicedesk.companyservice.PackageSubscription;
import com.businessos.modules.servicedesk.companyservice.PackageSubscriptionRepository;
import com.businessos.modules.servicedesk.companyservice.ServicePackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily sweep for PackageSubscription lifecycle transitions that ServicePackageServiceImpl
 * otherwise only performs on explicit API calls (activate/suspend/cancel/reactivate).
 * Runs across all tenants — a @Scheduled method never goes through DispatcherServlet, so
 * Hibernate's tenantFilter (enabled per-request by TenantFilterInterceptor) is never active
 * on this thread, same as the existing SubscriptionScheduler for platform-tier billing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PackageSubscriptionScheduler {

    private final PackageSubscriptionRepository subscriptionRepository;
    private final ServicePackageService servicePackageService;

    @Scheduled(cron = "0 15 0 * * *")
    public void processExpiredSubscriptions() {
        List<PackageSubscription> due = subscriptionRepository.findExpired(LocalDate.now());
        if (due.isEmpty()) return;

        int renewed = 0, expired = 0, failed = 0;
        for (PackageSubscription sub : due) {
            try {
                if (sub.isAutoRenew()) {
                    servicePackageService.renewSubscription(sub.getId());
                    renewed++;
                } else {
                    servicePackageService.expireSubscription(sub.getId());
                    expired++;
                }
            } catch (Exception ex) {
                failed++;
                log.error("Failed to process due package subscription {}: {}", sub.getId(), ex.getMessage());
            }
        }
        log.info("Package subscription sweep: {} renewed, {} expired, {} failed (of {} due)",
            renewed, expired, failed, due.size());
    }
}
