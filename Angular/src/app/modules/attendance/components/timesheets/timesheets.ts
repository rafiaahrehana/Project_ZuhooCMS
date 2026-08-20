import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Timesheet, TimesheetRequest } from '../../models/attendance.model';
import { TimesheetService } from '../../services/timesheet.service';
import { Employee } from '../../../hrm/models/hrm.model';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { PermissionService } from '../../../../core/services/permission.service';
import { StatCard } from '../../../../shared/components/stat-card/stat-card';
import { SpeechInputService } from '../../../../shared/services/speech-input.service';

@Component({
  selector: 'app-timesheets',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './timesheets.html',
})
export class Timesheets implements OnInit {
  @ViewChild('formSection') formSection?: ElementRef<HTMLElement>;

  /**
   * Whether this user reviews other people's hours.
   *
   * Only used for wording. An owner or manager still has their own entries on
   * this page, but calling the section "My Timesheets" to someone whose job
   * here is reviewing the team reads as if it were their personal log. Same
   * rule the leaves page follows.
   */
  readonly isReviewer: boolean;

  // OWN TIMESHEETS
  myTimesheets: Timesheet[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  showForm = false;
  editingId: number | null = null;
  form: TimesheetRequest = this.emptyForm();
  deleteTarget: Timesheet | null = null;

  employees: Employee[] = [];
  employeeIdFilter?: number;
  weekFilter = '';
  employeeTimesheets: Timesheet[] = [];
  employeePage = 0;
  employeeTotalPages = 0;
  employeeLoading = false;

  submitting = false;

  composeNotes = '';
  composing = false;
  composeError = '';

  constructor(
    private timesheetService: TimesheetService,
    private employeeService: EmployeeService,
    private permissions: PermissionService,
    private cdr: ChangeDetectorRef,
    private speechInput: SpeechInputService,
  ) {
    this.isReviewer = this.permissions.hasPermission('TIMESHEET_APPROVE');
  }

  get voiceSupported(): boolean {
    return this.speechInput.isSupported;
  }

  get listening(): boolean {
    return this.speechInput.isListening;
  }

  toggleVoiceInput(): void {
    if (this.speechInput.isListening) {
      this.speechInput.stop();
      return;
    }
    this.speechInput.start(
      (text) => { this.composeNotes = (this.composeNotes.trim() + ' ' + text).trim(); this.cdr.markForCheck(); },
      () => this.cdr.markForCheck(),
    );
    this.cdr.markForCheck();
  }

  ngOnInit(): void {
    this.load();
    this.employeeService.list(0, 500).subscribe({
      next: (res) => { this.employees = res.content; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load employees'; this.cdr.markForCheck(); },
    });
  }

  emptyForm(): TimesheetRequest {
    return { workDate: new Date().toISOString().slice(0, 10), hoursWorked: 0, billableHours: 0, projectName: '', taskDescription: '' };
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.timesheetService.listMine(this.page).subscribe({
      next: (res) => {
        this.myTimesheets = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load timesheets';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.composeNotes = '';
    this.composeError = '';
    this.showForm = true;
    this.scrollToForm();
  }

  composeWithAi(): void {
    const notes = this.composeNotes.trim();
    if (!notes || this.composing) return;
    this.composing = true;
    this.composeError = '';
    this.cdr.markForCheck();
    this.timesheetService.composeEntry({ projectName: this.form.projectName, roughNotes: notes }).subscribe({
      next: (res) => {
        this.form.taskDescription = res.taskDescription;
        this.form.description = res.description;
        this.composing = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.composeError = err?.error?.message || 'Could not draft that right now - please fill it in manually.';
        this.composing = false;
        this.cdr.markForCheck();
      },
    });
  }

  openEdit(t: Timesheet): void {
    if (t.status !== 'NOT_SUBMITTED') return;
    this.editingId = t.id;
    this.form = {
      workDate: t.workDate,
      startTime: t.startTime,
      endTime: t.endTime,
      hoursWorked: t.hoursWorked,
      billableHours: t.billableHours,
      projectName: t.projectName,
      taskDescription: t.taskDescription,
      description: t.description,
    };
    this.showForm = true;
    this.scrollToForm();
  }

  // The form renders above the stat cards, near the top of the page - without this,
  // clicking "edit" on a row further down (e.g. in Manager Review) leaves the form
  // open off-screen and looks like nothing happened.
  private scrollToForm(): void {
    setTimeout(() => this.formSection?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  save(): void {
    const op = this.editingId
      ? this.timesheetService.update(this.editingId, this.form)
      : this.timesheetService.log(this.form);
    op.subscribe({
      next: () => {
        this.showForm = false;
        this.success = this.editingId ? 'Timesheet updated' : 'Hours logged';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save timesheet'; this.cdr.markForCheck(); },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.timesheetService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Cannot delete an approved timesheet';
        this.cdr.markForCheck();
      },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  private isSameDate(dateStr: string, ref: Date): boolean {
    return dateStr === ref.toISOString().slice(0, 10);
  }

  private isInCurrentWeek(dateStr: string): boolean {
    const d = new Date(dateStr + 'T00:00:00');
    const now = new Date();
    const day = (now.getDay() + 6) % 7; // Monday = 0
    const monday = new Date(now);
    monday.setDate(now.getDate() - day);
    monday.setHours(0, 0, 0, 0);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    sunday.setHours(23, 59, 59, 999);
    return d >= monday && d <= sunday;
  }

  get todayHours(): number {
    const today = new Date();
    return this.myTimesheets
      .filter((t) => this.isSameDate(t.workDate, today))
      .reduce((sum, t) => sum + (t.hoursWorked || 0), 0);
  }

  get weekHours(): number {
    return this.myTimesheets
      .filter((t) => this.isInCurrentWeek(t.workDate))
      .reduce((sum, t) => sum + (t.hoursWorked || 0), 0);
  }

  get weekBillableHours(): number {
    return this.myTimesheets
      .filter((t) => this.isInCurrentWeek(t.workDate))
      .reduce((sum, t) => sum + (t.billableHours || 0), 0);
  }

  // Anything past a standard 40-hour week counts as overtime.
  get weekOvertimeHours(): number {
    return Math.max(0, this.weekHours - 40);
  }

  get pendingCount(): number {
    return this.myTimesheets.filter((t) => !t.approved).length;
  }

  get draftCount(): number {
    return this.myTimesheets.filter((t) => t.status === 'NOT_SUBMITTED').length;
  }

  // "My Recent Timesheets" (employee's own view) - a not-yet-submitted entry just
  // reads as "Pending" here; "Not Submitted" (see managerStatusLabel) is reserved
  // for the manager's side, where it means "nothing to review yet".
  statusLabel(t: Timesheet): string {
    if (t.status === 'APPROVED') return 'Approved';
    if (t.status === 'SUBMITTED') return 'Submitted';
    return 'Pending';
  }

  managerStatusLabel(t: Timesheet): string {
    if (t.status === 'APPROVED') return 'Approved';
    if (t.status === 'SUBMITTED') return 'Submitted';
    return 'Not Submitted';
  }

  statusBadgeClass(t: Timesheet): string {
    if (t.status === 'APPROVED') return 'badge-soft-success';
    if (t.status === 'SUBMITTED') return 'badge-soft-info';
    return 'badge-soft-warning';
  }

  submitForReview(): void {
    if (!this.draftCount || this.submitting) return;
    this.submitting = true;
    this.cdr.markForCheck();
    this.timesheetService.submitForReview().subscribe({
      next: (res) => {
        this.submitting = false;
        this.success = `${res.submitted} timesheet(s) submitted for review`;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.submitting = false;
        this.error = err?.error?.message || 'Failed to submit for review';
        this.cdr.markForCheck();
      },
    });
  }

  loadEmployeeTimesheets(): void {
    if (!this.employeeIdFilter) return;
    this.employeeLoading = true;
    this.cdr.markForCheck();

    if (this.weekFilter) {
      const { from, to } = this.weekRange(this.weekFilter);
      this.timesheetService.listByRange(this.employeeIdFilter, from, to).subscribe({
        next: (res) => {
          this.employeeTimesheets = res;
          this.employeeTotalPages = 1;
          this.employeePage = 0;
          this.employeeLoading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Failed to load timesheets for that employee';
          this.employeeLoading = false;
          this.cdr.markForCheck();
        },
      });
      return;
    }

    this.timesheetService.listForEmployee(this.employeeIdFilter, this.employeePage).subscribe({
      next: (res) => {
        this.employeeTimesheets = res.content;
        this.employeeTotalPages = res.totalPages;
        this.employeeLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load timesheets for that employee';
        this.employeeLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  // Converts an HTML5 <input type="week"> value (e.g. "2026-W29") into a Monday-Sunday date range.
  private weekRange(weekValue: string): { from: string; to: string } {
    const [yearStr, weekStr] = weekValue.split('-W');
    const year = Number(yearStr);
    const week = Number(weekStr);
    const jan4 = new Date(year, 0, 4);
    const jan4Day = (jan4.getDay() + 6) % 7;
    const monday = new Date(jan4);
    monday.setDate(jan4.getDate() - jan4Day + (week - 1) * 7);
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    return { from: monday.toISOString().slice(0, 10), to: sunday.toISOString().slice(0, 10) };
  }

  approve(t: Timesheet): void {
    this.timesheetService.approve(t.id).subscribe({
      next: () => {
        this.success = 'Timesheet approved';
        this.cdr.markForCheck();
        this.loadEmployeeTimesheets();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to approve'; this.cdr.markForCheck(); },
    });
  }

  goToEmployeePage(p: number): void {
    this.employeePage = p;
    this.loadEmployeeTimesheets();
  }
}
