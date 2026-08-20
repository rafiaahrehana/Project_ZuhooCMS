import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

/** Mirrors HrDashboardResponse. */
export interface HrDashboardSummary {
  totalEmployees: number;
  newHiresThisMonth: number;
  presentToday: number;
  onLeaveToday: number;
  absentToday: number;
  openPositions: number;
  /** Null when no attendance has been recorded today - render a dash, not 0%. */
  presentTodayPercent?: number | null;
  onLeaveTodayPercent?: number | null;
  /** Null when no payroll exists for the month. */
  monthlyPayrollTotal?: number | null;
  payrollMonth: number;
  payrollYear: number;
  departmentDistribution: DepartmentSlice[];
  leaveSummary: LeaveSummary;
  recentJoiners: JoinerItem[];
  upcomingItems: UpcomingItem[];
  headcountTrend: TrendPoint[];
  recruitmentPipeline: { stage: string; count: number }[];
  pendingApprovals: { id: number; employeeName: string; leaveType: string; startDate: string; endDate: string; totalDays: number }[];
}

export interface DepartmentSlice { department: string; count: number; percent: number; }
export interface LeaveSummary { total: number; approved: number; pending: number; rejected: number; }
export interface JoinerItem {
  employeeId: number; name: string; jobTitle?: string; department?: string; hireDate?: string;
}
export interface UpcomingItem {
  kind: 'BIRTHDAY' | 'PROBATION_END'; title: string; subtitle?: string; date: string; daysAway: number;
}
export interface TrendPoint { date: string; headcount: number; }

@Injectable({ providedIn: 'root' })
export class HrDashboardService {
  private readonly endpoint = '/hr/dashboard';

  constructor(private api: ApiService) {}

  /** Aggregated HR figures. Requires EMPLOYEE_VIEW. */
  summary(): Observable<HrDashboardSummary> {
    return this.api.get<HrDashboardSummary>(`${this.endpoint}/summary`);
  }
}
