package com.businessos.modules.dashboard;

import java.time.LocalDate;

public interface DashboardService {
    DashboardSummaryResponse getSummary(LocalDate from, LocalDate to);

    java.util.List<RecommendationResponse> getRecommendations();

    InsightsResponse getAiInsights();

    PlatformSummaryResponse getPlatformSummary();

    /** Daily history for the platform dashboard's sparklines/revenue chart - see PlatformMetricsSnapshot. */
    java.util.List<PlatformMetricsPoint> getPlatformMetricsHistory(int days);

    /** Upserts today's PlatformMetricsSnapshot - called by PlatformMetricsScheduler (daily cron + on startup). */
    void recordTodaysPlatformSnapshot();

    ClientSummaryResponse getClientSummary();
}
