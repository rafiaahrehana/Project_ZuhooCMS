package com.businessos.modules.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter @Builder
public class DashboardSummaryResponse {

    // CRM
    long totalLeads;
    long newLeads;
    long qualifiedLeads;
    long totalClients;
    long openOpportunities;
    BigDecimal pipelineValue;
    BigDecimal weightedForecast;

    // Servicedesk
    long pendingRequests;
    long inProgressRequests;
    long completedRequestsAllTime;
    long slaBreachedOpen;
    long totalServiceRequests;

    //  Support tickets
    long openTickets;
    long newTickets;

    //  Finance
    BigDecimal outstandingInvoiceAmount;
    BigDecimal walletBalance;
    BigDecimal walletCreditBalance;

    // HRM
    long totalEmployees;
    long pendingLeaveApprovals;
    long payrollProcessedThisMonth;

    // Trends & Extras
    double leadsTrend;
    double clientsTrend;
    double opportunitiesTrend;
    double weightedForecastTrend;

    java.util.List<String> salesOverviewLabels;
    java.util.List<Long> salesOverviewData;
    double salesOverviewTrend;

    long serviceDeskPendingCount;
    long serviceDeskInProgressCount;
    long serviceDeskResolvedCount;
    long serviceDeskOnHoldCount;

    long tasksCreatedCount;
    double tasksCreatedTrend;
    long tasksCompletedCount;
    double tasksCompletedTrend;
    long tasksOverdueCount;
    double tasksOverdueTrend;

    java.util.List<InvoiceDetailDto> overdueInvoices;

    double walletBalanceTrend;
    BigDecimal walletCredits;
    BigDecimal walletDebits;

    long employeesPresentToday;
    long employeesOnLeave;
    double employeesTrend;

    java.util.List<AnnouncementDto> announcements;
}
