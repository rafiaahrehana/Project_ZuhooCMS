import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ATTENDANCE_STATUSES, AttendanceRecord, ManualAttendanceRequest } from '../../models/attendance.model';
import { AttendanceService } from '../../services/attendance.service';
import { AuthService } from '../../../../core/services/auth.service';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { Employee } from '../../../hrm/models/hrm.model';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-attendance-list',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './attendance-list.html',
})
export class AttendanceList implements OnInit {
  records: AttendanceRecord[] = [];
  employees: Employee[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';
  dateFilter = '';
  searchQuery = '';
  statuses = ATTENDANCE_STATUSES;

  showForm = false;
  saving = false;
  form: Partial<ManualAttendanceRequest> = {};

  /** Record open in the full-detail modal - every field the backend tracks. */
  viewing: AttendanceRecord | null = null;

  /** Mirrors the backend ShiftType enum. */
  readonly shiftTypes = ['MORNING', 'AFTERNOON', 'FULL_DAY', 'EVENING', 'NIGHT', 'FLEXIBLE'];

  // Owner-only "Backfill absentees" action
  isOwner = false;
  showBackfill = false;
  backfilling = false;
  backfillStart = '';
  backfillEnd = '';

  get consolidatedRecords(): AttendanceRecord[] {
    const map = new Map<string, AttendanceRecord>();
    for (const r of this.records) {
      const key = `${r.employeeId || r.employeeName}_${r.attendanceDate}`;
      if (!map.has(key)) {
        map.set(key, { ...r });
      } else {
        const existing = map.get(key)!;
        if (!existing.checkInTime && r.checkInTime) {
          existing.checkInTime = r.checkInTime;
        }
        if (!existing.checkOutTime && r.checkOutTime) {
          existing.checkOutTime = r.checkOutTime;
        }
        if (existing.status === 'ABSENT' && r.status !== 'ABSENT') {
          existing.status = r.status;
        }
        if (r.isLate) {
          existing.isLate = true;
          existing.lateMinutes = r.lateMinutes;
        }
        if (r.approved) {
          existing.approved = true;
        }
        if (existing.checkInTime && existing.checkOutTime) {
          existing.totalWorkingHours = this.calcHours(existing.checkInTime, existing.checkOutTime);
        }
      }
    }
    return Array.from(map.values());
  }

  private calcHours(inTime: string, outTime: string): number {
    try {
      const [h1, m1] = inTime.split(':').map(Number);
      const [h2, m2] = outTime.split(':').map(Number);
      const mins = (h2 * 60 + m2) - (h1 * 60 + m1);
      return mins > 0 ? Number((mins / 60).toFixed(2)) : 0;
    } catch {
      return 0;
    }
  }

  constructor(
    private attendanceService: AttendanceService,
    private employeeService: EmployeeService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.isOwner = this.auth.hasRole('COMPANY_OWNER');
    this.load();
    this.employeeService.list(0, 500).subscribe({ next: (res) => { this.employees = res.content; this.cdr.markForCheck(); } });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.attendanceService
      .list(
        this.page,
        20,
        this.statusFilter || undefined,
        this.dateFilter || undefined,
        undefined,
        undefined,
        this.searchQuery || undefined
      )
      .subscribe({
        next: (res) => {
          this.records = res?.content ?? [];
          this.totalPages = res?.totalPages ?? 0;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Failed to load records';
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }

  onFilterChange(): void {
    this.page = 0;
    this.load();
  }

  clearFilters(): void {
    this.dateFilter = '';
    this.statusFilter = '';
    this.searchQuery = '';
    this.page = 0;
    this.load();
  }

  approve(r: AttendanceRecord): void {
    this.attendanceService.approve(r.id).subscribe({
      next: () => {
        this.success = 'Approved';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  openAdd(): void {
    this.form = {
      attendanceDate: new Date().toISOString().slice(0, 10),
      status: 'PRESENT',
    };
    this.error = '';
    this.showForm = true;
  }

  setNow(field: 'checkInTime' | 'checkOutTime'): void {
    const now = new Date();
    const hh = String(now.getHours()).padStart(2, '0');
    const mm = String(now.getMinutes()).padStart(2, '0');
    this.form[field] = `${hh}:${mm}`;
  }

  save(): void {
    if (!this.form.employeeId || !this.form.attendanceDate || !this.form.status) return;
    this.saving = true;
    this.error = '';
    this.attendanceService.createManual(this.form as ManualAttendanceRequest).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = 'Attendance recorded';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to record attendance';
        this.cdr.markForCheck();
      },
    });
  }

  openBackfill(): void {
    const today = new Date();
    const weekAgo = new Date();
    weekAgo.setDate(today.getDate() - 7);
    this.backfillStart = weekAgo.toISOString().slice(0, 10);
    this.backfillEnd = today.toISOString().slice(0, 10);
    this.error = '';
    this.showBackfill = true;
  }

  runBackfill(): void {
    if (!this.backfillStart || !this.backfillEnd) return;
    if (this.backfillStart > this.backfillEnd) {
      this.error = 'Start date must not be after end date';
      return;
    }
    this.backfilling = true;
    this.error = '';
    this.attendanceService.backfillAbsentees(this.backfillStart, this.backfillEnd).subscribe({
      next: (res) => {
        this.backfilling = false;
        this.showBackfill = false;
        this.success = res.created > 0
          ? `Marked ${res.created} absentee record${res.created === 1 ? '' : 's'} for ${res.startDate} → ${res.endDate}.`
          : `No new absences found for ${res.startDate} → ${res.endDate}.`;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.backfilling = false;
        this.error = err?.error?.error || err?.error?.message || 'Failed to backfill absentees';
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Late duration as a human reading: "45 min" under an hour, "1h 15m" over it.
   *
   * Raw minutes stop being readable past an hour or so - "95m" makes you do the
   * arithmetic yourself, which is exactly what a payroll or HR reviewer should
   * not have to do while scanning a list.
   */
  /**
   * Whether a row should show a late duration at all.
   *
   * Lateness only means something on a day that was actually worked. A day
   * settled as ABSENT - including the check-in-with-no-check-out case, which is
   * settled to ABSENT overnight - is not paid, so showing "20 min" beside it
   * reads as if the employee were both absent and late for the same day.
   *
   * The backend clears the flag when it settles a day, so this is belt and
   * braces for rows written before that fix and for any manual entry that sets
   * both by hand.
   */
  showsLate(r: { isLate?: boolean; lateMinutes?: number | null; status?: string }): boolean {
    if (!r.isLate || !r.lateMinutes) return false;
    return r.status !== 'ABSENT' && r.status !== 'ON_LEAVE' && r.status !== 'HOLIDAY';
  }

  lateLabel(minutes: number | null | undefined): string {
    const total = Math.max(0, Math.round(minutes ?? 0));
    if (total === 0) return '';
    if (total < 60) return `${total} min`;

    const hours = Math.floor(total / 60);
    const remainder = total % 60;
    return remainder === 0 ? `${hours}h` : `${hours}h ${remainder}m`;
  }

  statusClass(s: string): string {
    return (
      {
        PRESENT: 'text-bg-success',
        LATE: 'text-bg-warning',
        ABSENT: 'text-bg-danger',
        ON_LEAVE: 'text-bg-info',
        HALF_DAY: 'text-bg-secondary',
        WORK_FROM_HOME: 'text-bg-info',
        WEEKEND: 'text-bg-light',
        HOLIDAY: 'text-bg-light',
        PARTIAL_DAY: 'text-bg-warning',
        UNMARKED: 'text-bg-light',
      }[s] || 'text-bg-light'
    );
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
