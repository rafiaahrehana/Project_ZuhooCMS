import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, PagedResponse } from '../../../../core/services/api.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

export interface InterviewRow {
  id: number;
  jobApplicationId: number;
  applicantName: string;
  jobTitle?: string;
  applicationStatus?: string;
  round: string;
  scheduledAt: string;
  durationMinutes?: number;
  mode?: string;
  meetingLink?: string;
  interviewerId?: number;
  interviewerName?: string;
  status: string;
  rating?: number;
  strengths?: string;
  concerns?: string;
  recommendation?: string;
  feedbackAt?: string;
}

@Component({
  selector: 'app-interviews',
  imports: [CommonModule, FormsModule, Loader, EmptyState, Pagination, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './interviews.html',
})
export class Interviews implements OnInit {
  interviews: InterviewRow[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';

  readonly rounds = ['SCREENING', 'TECHNICAL', 'HR', 'FINAL'];
  readonly modes = ['VIDEO', 'ONSITE', 'PHONE'];
  readonly recommendations = ['STRONG_HIRE', 'HIRE', 'NEUTRAL', 'NO_HIRE', 'STRONG_NO_HIRE'];
  readonly statuses = ['SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];

  // Schedule / reschedule modal
  showForm = false;
  saving = false;
  editingId: number | null = null;
  form: any = {};
  applications: { id: number; applicantName: string; jobTitle?: string }[] = [];
  employees: { id: number; firstName: string; lastName: string }[] = [];

  // Feedback modal
  feedbackTarget: InterviewRow | null = null;
  feedbackForm: any = {};
  savingFeedback = false;

  cancelTarget: InterviewRow | null = null;

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
    // Open applications for the schedule form; employees as interviewers.
    this.api.get<PagedResponse<any>>('/recruitment/applications', { page: 0, size: 200 }).subscribe({
      next: (res) => {
        this.applications = (res.content || [])
          .filter((a: any) => !['HIRED', 'REJECTED', 'WITHDRAWN'].includes(a.status))
          .map((a: any) => ({ id: a.id, applicantName: a.candidateName, jobTitle: a.jobPostingTitle || a.jobTitle }));
        this.cdr.markForCheck();
      },
      error: () => {},
    });
    this.api.get<PagedResponse<any>>('/employees', { page: 0, size: 500 }).subscribe({
      next: (res) => { this.employees = res.content || []; this.cdr.markForCheck(); },
      error: () => {},
    });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    const params: any = { page: this.page, size: 20 };
    if (this.statusFilter) params.status = this.statusFilter;
    this.api.get<PagedResponse<InterviewRow>>('/recruitment/interviews', params).subscribe({
      next: (res) => {
        this.interviews = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.error = 'Failed to load interviews'; this.loading = false; this.cdr.markForCheck(); },
    });
  }

  openSchedule(): void {
    this.editingId = null;
    this.form = { round: 'SCREENING', mode: 'VIDEO', durationMinutes: 60 };
    this.error = '';
    this.showForm = true;
  }

  openEdit(row: InterviewRow): void {
    this.editingId = row.id;
    this.form = {
      jobApplicationId: row.jobApplicationId,
      round: row.round,
      scheduledAt: row.scheduledAt?.slice(0, 16),
      durationMinutes: row.durationMinutes,
      mode: row.mode,
      meetingLink: row.meetingLink,
      interviewerId: row.interviewerId ?? null,
    };
    this.error = '';
    this.showForm = true;
  }

  save(): void {
    if (!this.editingId && !this.form.jobApplicationId) { this.error = 'Pick an application'; return; }
    if (!this.form.scheduledAt) { this.error = 'Pick a date and time'; return; }
    this.saving = true;
    this.error = '';
    const payload = { ...this.form };
    const op = this.editingId
      ? this.api.put<InterviewRow>(`/recruitment/interviews/${this.editingId}`, payload)
      : this.api.post<InterviewRow>('/recruitment/interviews', payload);
    op.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.editingId = null;
        this.success = 'Interview saved';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save interview';
        this.cdr.markForCheck();
      },
    });
  }

  openFeedback(row: InterviewRow): void {
    this.feedbackTarget = row;
    this.feedbackForm = {
      rating: row.rating ?? null,
      strengths: row.strengths || '',
      concerns: row.concerns || '',
      recommendation: row.recommendation || null,
      noShow: row.status === 'NO_SHOW',
    };
  }

  saveFeedback(): void {
    if (!this.feedbackTarget) return;
    this.savingFeedback = true;
    this.api.patch<InterviewRow>(`/recruitment/interviews/${this.feedbackTarget.id}/feedback`, this.feedbackForm).subscribe({
      next: () => {
        this.savingFeedback = false;
        this.feedbackTarget = null;
        this.success = 'Feedback recorded';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.savingFeedback = false;
        this.error = err?.error?.message || 'Failed to save feedback';
        this.feedbackTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  doCancel(): void {
    if (!this.cancelTarget) return;
    this.api.patch(`/recruitment/interviews/${this.cancelTarget.id}/cancel`, {}).subscribe({
      next: () => { this.cancelTarget = null; this.success = 'Interview cancelled'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cancelTarget = null; this.cdr.markForCheck(); },
    });
  }

  goToPage(p: number): void { this.page = p; this.load(); }

  statusBadge(s: string): string {
    return { SCHEDULED: 'text-bg-info', COMPLETED: 'text-bg-success', CANCELLED: 'text-bg-secondary', NO_SHOW: 'text-bg-danger' }[s] || 'text-bg-light';
  }

  recommendationBadge(r?: string): string {
    return {
      STRONG_HIRE: 'text-bg-success', HIRE: 'badge-soft-success', NEUTRAL: 'text-bg-light border',
      NO_HIRE: 'badge-soft-warning', STRONG_NO_HIRE: 'text-bg-danger',
    }[r || ''] || 'text-bg-light';
  }

  label(v?: string): string {
    return (v || '').replaceAll('_', ' ');
  }
}
