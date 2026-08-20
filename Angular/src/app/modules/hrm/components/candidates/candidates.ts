import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApplicationStatus, Candidate, CandidateRequest, JobApplication } from '../../models/hrm.model';
import { CandidateService } from '../../services/candidate.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-candidates',
  imports: [CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './candidates.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Candidates implements OnInit {
  candidates: Candidate[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  q = '';

  // Detail panel: one candidate, all their applications across jobs.
  detailTarget: Candidate | null = null;
  detailApplications: JobApplication[] = [];
  detailLoading = false;

  editTarget: Candidate | null = null;
  editForm: CandidateRequest = {};
  saving = false;

  deleteTarget: Candidate | null = null;

  constructor(private candidateService: CandidateService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.error = '';
    this.candidateService.list(this.page, 20, this.q.trim() || undefined).subscribe({
      next: (res) => { this.candidates = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load candidates'; this.loading = false; this.cdr.markForCheck(); },
    });
  }

  search(): void { this.page = 0; this.load(); }

  goToPage(p: number): void { this.page = p; this.load(); }

  openDetail(c: Candidate): void {
    this.detailTarget = c;
    this.detailApplications = [];
    this.detailLoading = true;
    this.candidateService.applications(c.id).subscribe({
      next: (apps) => { this.detailApplications = apps; this.detailLoading = false; this.cdr.markForCheck(); },
      error: () => { this.detailLoading = false; this.cdr.markForCheck(); },
    });
  }

  openEdit(c: Candidate): void {
    this.editTarget = c;
    this.editForm = { ...c };
    this.error = '';
  }

  save(): void {
    if (!this.editTarget) return;
    this.saving = true;
    this.candidateService.update(this.editTarget.id, this.editForm).subscribe({
      next: () => {
        this.saving = false;
        this.editTarget = null;
        this.success = 'Candidate updated';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to update candidate';
        this.cdr.markForCheck();
      },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.candidateService.delete(this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Candidate removed'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.deleteTarget = null; this.error = err?.error?.message || 'Failed to remove candidate'; this.cdr.markForCheck(); },
    });
  }

  skillList(c: Candidate): string[] {
    return (c.skills || '').split(',').map((s) => s.trim()).filter(Boolean);
  }

  initials(c: Candidate): string {
    const parts = c.name.trim().split(/\s+/);
    return ((parts[0]?.[0] || '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
  }

  statusClass(status: ApplicationStatus): string {
    return {
      APPLIED: 'text-bg-secondary',
      SCREENING: 'text-bg-info',
      SHORTLISTED: 'text-bg-info',
      INTERVIEW_SCHEDULED: 'text-bg-primary',
      INTERVIEWED: 'text-bg-primary',
      SELECTED: 'text-bg-primary',
      OFFER_PENDING: 'text-bg-warning',
      OFFER_SENT: 'text-bg-warning',
      OFFER_ACCEPTED: 'text-bg-success',
      OFFER_REJECTED: 'text-bg-danger',
      HIRED: 'text-bg-success',
      REJECTED: 'text-bg-danger',
      WITHDRAWN: 'text-bg-secondary',
    }[status] || 'text-bg-secondary';
  }
}
