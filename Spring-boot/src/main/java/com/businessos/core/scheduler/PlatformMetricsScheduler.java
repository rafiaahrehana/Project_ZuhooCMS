package com.businessos.core.scheduler;

import com.businessos.modules.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlatformMetricsScheduler implements CommandLineRunner {

    private final DashboardService dashboardService;

    /** Seeds today's snapshot immediately on boot, so the dashboard's sparklines
     * have at least one real data point instead of sitting empty until the first
     * midnight cron run. */
    @Override
    public void run(String... args) {
        dashboardService.recordTodaysPlatformSnapshot();
    }

    // Runs after SubscriptionScheduler.suspendExpiredCompanies (00:05) so the day's
    // snapshot reflects any status changes that job just made.
    @Scheduled(cron = "0 10 0 * * *")
    public void recordDailySnapshot() {
        dashboardService.recordTodaysPlatformSnapshot();
    }
}
