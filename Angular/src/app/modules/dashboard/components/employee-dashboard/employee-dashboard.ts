import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { PermissionService } from '../../../../core/services/permission.service';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { AnnouncementService } from '../../../hrm/services/announcement.service';
import { HolidayService } from '../../../hrm/services/holiday.service';
import { AttendanceService } from '../../../attendance/services/attendance.service';
import { LeaveBalanceService } from '../../../hrm/services/leave-balance.service';
import { PerformanceReviewService } from '../../../hrm/services/performance-review.service';
import { ServiceRequestService } from '../../../servicedesk/services/service-request.service';
import { Employee, LeaveBalance } from '../../../hrm/models/hrm.model';
import { ServiceRequest } from '../../../servicedesk/models/servicedesk.model';
import { AttendanceRecord, MyAttendanceMonthlySummary } from '../../../attendance/models/attendance.model';
import { Loader } from '../../../../shared/components/loader/loader';

interface NoticeItem {
  title: string;
  date: string;
  kind: 'Announcement' | 'Holiday';
}

interface HolidayItem {
  name: string;
  date: string;
  daysAway: number;
}

/** Quick action tile. Plain navigation - no state, so no permission gating beyond the route guard. */
interface QuickAction {
  label: string;
  icon: string;
  link: string;
  accent: string;
}

@Component({
  selector: 'app-employee-dashboard',
  imports: [CommonModule, RouterLink, Loader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './employee-dashboard.html',
})
export class EmployeeDashboard implements OnInit {
  profile?: Employee;
  todayRecord?: AttendanceRecord;
  monthlySummary?: MyAttendanceMonthlySummary;
  notices: NoticeItem[] = [];

  // Panels added for the redesign, each backed by a real endpoint.
  leaveBalances: LeaveBalance[] = [];
  myRequests: ServiceRequest[] = [];
  latestReviewScore: number | null = null;

  /** Captured once so the header date doesn't change mid-session. */
  readonly today = new Date();

  loadingProfile = false;
  loadingAttendance = false;
  checkingInOut = false;
  error = '';
  success = '';

  readonly quickActions: QuickAction[] = [
    { label: 'Apply for Leave',   icon: 'bi-calendar-plus',  link: '/hrm/leaves',          accent: '#8b5cf6' },
    { label: 'My Timesheet',      icon: 'bi-clock-history',  link: '/attendance/timesheets', accent: '#2563eb' },
    { label: 'My Payslips',       icon: 'bi-cash-stack',     link: '/hrm/my-payslips',     accent: '#0d9488' },
    { label: 'Raise a Request',   icon: 'bi-plus-circle',    link: '/servicedesk/requests', accent: '#f59e0b' },
    { label: 'Attendance History',icon: 'bi-calendar-check', link: '/attendance/records',  accent: '#6366f1' },
    { label: 'My Profile',        icon: 'bi-person-gear',    link: '/my-profile',          accent: '#e11d48' },
  ];

  constructor(
    public auth: AuthService,
    public permissionService: PermissionService,
    private employeeService: EmployeeService,
    private announcementService: AnnouncementService,
    private holidayService: HolidayService,
    private attendanceService: AttendanceService,
    private leaveBalanceService: LeaveBalanceService,
    private performanceService: PerformanceReviewService,
    private requestService: ServiceRequestService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadProfile();
    this.loadToday();
    this.loadMonthlySummary();
    this.loadNoticeBoard();
    this.loadLeaveBalances();
    this.loadMyRequests();
  }

  // ── Derived figures for the stat cards ──────────────────────
  // Every one returns null when the underlying data is absent, so the template
  // renders a dash. A zero would read as a real measurement.

  /** Percentage of recorded working days attended this month. */
  get attendancePercent(): number | null {
    const s = this.monthlySummary;
    if (!s) return null;
    const attended = (s.presentDays ?? 0) + (s.halfDays ?? 0);
    const working = attended + (s.absentDays ?? 0) + (s.onLeaveDays ?? 0);
    if (working === 0) return null;
    return Math.round((attended / working) * 1000) / 10;
  }

  /** Total leave days still available across all types. */
  get leaveDaysAvailable(): number | null {
    if (!this.leaveBalances.length) return null;
    return this.leaveBalances.reduce((sum, b) => sum + this.availableDays(b), 0);
  }

  /** Open service requests assigned to me. */
  get openRequestCount(): number {
    return this.myRequests.filter(r =>
      r.status !== 'COMPLETED' && r.status !== 'CANCELLED' && r.status !== 'REJECTED').length;
  }

  /**
   * The server already computes this as max(0, entitled - used - pending), so
   * take its value rather than recomputing and risking the two drifting apart.
   */
  availableDays(b: LeaveBalance): number {
    return b.remainingDays ?? 0;
  }

  /** Used + pending as a share of entitlement, for the progress bars. */
  usedPercent(b: LeaveBalance): number {
    const entitled = b.entitledDays ?? 0;
    if (entitled <= 0) return 0;
    const consumed = (b.usedDays ?? 0) + (b.pendingDays ?? 0);
    return Math.min(100, Math.round((consumed / entitled) * 100));
  }

  /** Upcoming holidays with a countdown, soonest first. */
  get upcomingHolidays(): HolidayItem[] {
    const today = new Date(); today.setHours(0, 0, 0, 0);
    return this.notices
      .filter(n => n.kind === 'Holiday')
      .map(n => ({
        name: n.title,
        date: n.date,
        daysAway: Math.round((new Date(n.date).getTime() - today.getTime()) / 86400000),
      }))
      .sort((a, b) => a.daysAway - b.daysAway)
      .slice(0, 4);
  }

  get announcementItems(): NoticeItem[] {
    return this.notices.filter(n => n.kind === 'Announcement').slice(0, 3);
  }

  loadLeaveBalances(): void {
    this.leaveBalanceService.listMine().subscribe({
      next: (list) => { this.leaveBalances = list || []; this.cdr.markForCheck(); },
      // No balances configured yet is normal, not an error worth showing.
      error: () => { this.leaveBalances = []; this.cdr.markForCheck(); },
    });
  }

  loadMyRequests(): void {
    this.requestService.assignedToMe(0, 5).subscribe({
      next: (res) => {
        this.myRequests = res.content || [];
        this.cdr.markForCheck();
        this.loadLatestReview();
      },
      error: () => { this.myRequests = []; this.cdr.markForCheck(); this.loadLatestReview(); },
    });
  }

  /** Needs the employee id, so it runs once the profile has arrived. */
  private loadLatestReview(): void {
    const id = this.profile?.id;
    if (!id) return;
    this.performanceService.listForEmployee(id, 0, 1).subscribe({
      next: (res) => {
        const latest = (res.content || [])[0];
        this.latestReviewScore = latest?.overallScore ?? null;
        this.cdr.markForCheck();
      },
      error: () => { this.latestReviewScore = null; this.cdr.markForCheck(); },
    });
  }

  get roleLabel(): string {
    return this.profile?.customRoleName || 'Employee';
  }

  loadProfile(): void {
    this.loadingProfile = true;
    this.employeeService.getMyProfile().subscribe({
      next: (p) => {
        this.profile = p;
        this.loadingProfile = false;
        this.cdr.markForCheck();
        // The review lookup is keyed on the employee id, which only exists now.
        this.loadLatestReview();
      },
      error: () => { this.loadingProfile = false; this.cdr.markForCheck(); },
    });
  }

  loadToday(): void {
    this.attendanceService.myToday().subscribe({
      next: (rec) => { this.todayRecord = rec || undefined; this.cdr.markForCheck(); },
      error: () => { this.todayRecord = undefined; this.cdr.markForCheck(); },
    });
  }

  loadMonthlySummary(): void {
    this.loadingAttendance = true;
    this.attendanceService.myMonthlySummary().subscribe({
      next: (s) => { this.monthlySummary = s; this.loadingAttendance = false; this.cdr.markForCheck(); },
      error: () => { this.loadingAttendance = false; this.cdr.markForCheck(); },
    });
  }

  loadNoticeBoard(): void {
    const today = new Date().toISOString().slice(0, 10);
    this.announcementService.listActive().subscribe({
      next: (list) => {
        const items: NoticeItem[] = (list || []).map(a => ({
          title: a.title, date: a.publishedAt || a.createdAt, kind: 'Announcement' as const,
        }));
        this.notices = [...this.notices, ...items].sort((a, b) => (a.date < b.date ? 1 : -1)).slice(0, 6);
        this.cdr.markForCheck();
      },
      error: () => { /* no ANNOUNCEMENT_VIEW or none active - notice board just shows holidays */ },
    });
    this.holidayService.listCurrentYear().subscribe({
      next: (list) => {
        const upcoming: NoticeItem[] = (list || [])
          .filter(h => h.holidayDate >= today)
          .map(h => ({ title: h.name, date: h.holidayDate, kind: 'Holiday' as const }));
        this.notices = [...this.notices, ...upcoming].sort((a, b) => (a.date < b.date ? 1 : -1)).slice(0, 6);
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  /** Late duration as a human reading: "45 min" under an hour, "1h 15m" over it. */
  lateLabel(minutes: number | null | undefined): string {
    const total = Math.max(0, Math.round(minutes ?? 0));
    if (total === 0) return '';
    if (total < 60) return `${total} min`;

    const hours = Math.floor(total / 60);
    const remainder = total % 60;
    return remainder === 0 ? `${hours}h` : `${hours}h ${remainder}m`;
  }

  checkIn(): void {
    this.checkingInOut = true;
    this.error = '';
    const timeStr = new Date().toTimeString().split(' ')[0];
    this.attendanceService.checkIn({ checkInTime: timeStr, method: 'MANUAL' }).subscribe({
      next: (r) => {
        this.todayRecord = r;
        this.checkingInOut = false;
        this.success = 'Checked in successfully';
        this.cdr.markForCheck();
        this.loadMonthlySummary();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Check-in failed';
        this.checkingInOut = false;
        this.cdr.markForCheck();
      },
    });
  }

  checkOut(): void {
    if (!this.todayRecord) return;
    this.checkingInOut = true;
    this.error = '';
    const timeStr = new Date().toTimeString().split(' ')[0];
    this.attendanceService.checkOut(this.todayRecord.id, { checkOutTime: timeStr, method: 'MANUAL' }).subscribe({
      next: (r) => {
        this.todayRecord = r;
        this.checkingInOut = false;
        this.success = 'Checked out successfully';
        this.cdr.markForCheck();
        this.loadMonthlySummary();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Check-out failed';
        this.checkingInOut = false;
        this.cdr.markForCheck();
      },
    });
  }
}
