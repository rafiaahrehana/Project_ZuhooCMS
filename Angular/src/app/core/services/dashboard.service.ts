import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface InvoiceDetailDto {
  invoiceNumber: string;
  clientName: string;
  amount: number;
  daysOverdue: number;
}

export interface AnnouncementDto {
  title: string;
  content: string;
  timeAgo: string;
}

export interface DashboardSummary {
  totalLeads: number;
  newLeads: number;
  qualifiedLeads: number;
  totalClients: number;
  openOpportunities: number;
  pipelineValue: number;
  weightedForecast: number;
  pendingRequests: number;
  inProgressRequests: number;
  completedRequestsAllTime: number;
  slaBreachedOpen: number;
  totalServiceRequests: number;
  openTickets: number;
  newTickets: number;
  outstandingInvoiceAmount: number;
  walletBalance: number;
  walletCreditBalance: number;
  totalEmployees: number;
  pendingLeaveApprovals: number;
  payrollProcessedThisMonth: number;

  leadsTrend: number;
  clientsTrend: number;
  opportunitiesTrend: number;
  weightedForecastTrend: number;
  
  salesOverviewLabels: string[];
  salesOverviewData: number[];
  salesOverviewTrend: number;
  
  serviceDeskPendingCount: number;
  serviceDeskInProgressCount: number;
  serviceDeskResolvedCount: number;
  serviceDeskOnHoldCount: number;
  
  tasksCreatedCount: number;
  tasksCreatedTrend: number;
  tasksCompletedCount: number;
  tasksCompletedTrend: number;
  tasksOverdueCount: number;
  tasksOverdueTrend: number;
  
  overdueInvoices: InvoiceDetailDto[];
  
  walletBalanceTrend: number;
  walletCredits: number;
  walletDebits: number;
  
  employeesPresentToday: number;
  employeesOnLeave: number;
  employeesTrend: number;
  
  announcements: AnnouncementDto[];
}

// Mirrors backend PlanCompanyCount
export interface PlanCompanyCount {
  code: string;
  name: string;
  count: number;
}

// Mirrors backend PlatformSummaryResponse (GET /api/dashboard/platform-summary)
export interface PlatformSummary {
  totalCompanies: number;
  activeCompanies: number;
  trialCompanies: number;
  suspendedCompanies: number;
  pendingVerificationCompanies: number;
  trialsExpiringWithin7Days: number;
  companiesByPlan: PlanCompanyCount[];
  totalPlatformUsers: number;
  totalRevenue: number;
  revenueThisMonth: number;
}

// Mirrors backend PlatformMetricsPoint (GET /api/dashboard/platform-metrics-history)
export interface PlatformMetricsPoint {
  date: string;
  totalCompanies: number;
  activeCompanies: number;
  trialCompanies: number;
  suspendedCompanies: number;
  revenue: number;
}

// Mirrors backend ClientSummaryResponse (GET /api/dashboard/client-summary)
export interface ClientSummary {
  pendingRequests: number;
  inProgressRequests: number;
  completedRequests: number;
  unpaidInvoices: number;
  outstandingInvoiceAmount: number;
}

// Mirrors backend RecommendationResponse
export interface RecommendationResponse {
  type: string;
  severity: string;
  message: string;
  link: string;
}

// Mirrors backend InsightsResponse
export interface InsightsResponse {
  insights: string;
  generatedInMs: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private api: ApiService) {}

  getSummary(from?: string, to?: string): Observable<DashboardSummary> {
    return this.api.get<DashboardSummary>('/dashboard/summary', { from, to });
  }

  getPlatformSummary(): Observable<PlatformSummary> {
    return this.api.get<PlatformSummary>('/dashboard/platform-summary');
  }

  getPlatformMetricsHistory(days: number): Observable<PlatformMetricsPoint[]> {
    return this.api.get<PlatformMetricsPoint[]>('/dashboard/platform-metrics-history', { days });
  }

  getClientSummary(): Observable<ClientSummary> {
    return this.api.get<ClientSummary>('/dashboard/client-summary');
  }

  getRecommendations(): Observable<RecommendationResponse[]> {
    return this.api.get<RecommendationResponse[]>('/dashboard/recommendations');
  }

  getInsights(): Observable<InsightsResponse> {
    return this.api.get<InsightsResponse>('/dashboard/insights');
  }
}
