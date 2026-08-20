import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, PagedResponse } from '../../../../core/services/api.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

export interface PoolCandidate {
  id: number;
  name: string;
  email: string;
  phone?: string;
  resumeUrl?: string;
  linkedInUrl?: string;
  desiredRole?: string;
  skills?: string;
  rating?: number;
  reason: string;
  notes?: string;
  sourceApplicationId?: number;
  sourceJobTitle?: string;
  createdAt: string;
}

@Component({
  selector: 'app-talent-pool',
  imports: [CommonModule, FormsModule, Loader, EmptyState, Pagination, HasPermissionDirective, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './talent-pool.html',
})
export class TalentPool implements OnInit {
  candidates: PoolCandidate[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  keyword = '';

  readonly reasons = ['DECLINED_OFFER', 'NO_VACANCY', 'FUTURE_FIT', 'WITHDREW', 'REFERRAL', 'OTHER'];

  showForm = false;
  saving = false;
  editingId: number | null = null;
  form: any = {};
  deleteTarget: PoolCandidate | null = null;

  /** Closed applications not yet pooled - offered by the "From application" picker. */
  closedApplications: { id: number; applicantName: string; status: string }[] = [];
  poolFromApplicationId: number | null = null;

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
    this.api.get<PagedResponse<any>>('/recruitment/applications', { page: 0, size: 200 }).subscribe({
      next: (res) => {
        this.closedApplications = (res.content || [])
          .filter((a: any) => ['REJECTED', 'WITHDRAWN', 'OFFER_REJECTED', 'INTERVIEWED'].includes(a.status))
          .map((a: any) => ({ id: a.id, applicantName: a.candidateName, status: a.status }));
        this.cdr.markForCheck();
      },
      error: () => {},
    });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    const params: any = { page: this.page, size: 24 };
    if (this.keyword.trim()) params.keyword = this.keyword.trim();
    this.api.get<PagedResponse<PoolCandidate>>('/recruitment/talent-pool', params).subscribe({
      next: (res) => { this.candidates = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load talent pool'; this.loading = false; this.cdr.markForCheck(); },
    });
  }

  openAdd(): void {
    this.editingId = null;
    this.form = { reason: 'FUTURE_FIT' };
    this.poolFromApplicationId = null;
    this.error = '';
    this.showForm = true;
  }

  openEdit(c: PoolCandidate): void {
    this.editingId = c.id;
    this.form = { ...c };
    this.poolFromApplicationId = null;
    this.error = '';
    this.showForm = true;
  }

  save(): void {
    // Pooling from a closed application copies its contact details server-side.
    if (!this.editingId && this.poolFromApplicationId) {
      this.saving = true;
      this.api.post<PoolCandidate>(`/recruitment/talent-pool/from-application/${this.poolFromApplicationId}`, {
        reason: this.form.reason, notes: this.form.notes,
      }).subscribe({
        next: () => this.afterSave('Candidate pooled from application'),
        error: (err) => this.saveFailed(err),
      });
      return;
    }
    if (!this.form.name?.trim() || !this.form.email?.trim()) { this.error = 'Name and email are required'; return; }
    this.saving = true;
    const op = this.editingId
      ? this.api.put<PoolCandidate>(`/recruitment/talent-pool/${this.editingId}`, this.form)
      : this.api.post<PoolCandidate>('/recruitment/talent-pool', this.form);
    op.subscribe({
      next: () => this.afterSave('Candidate saved'),
      error: (err) => this.saveFailed(err),
    });
  }

  private afterSave(message: string): void {
    this.saving = false;
    this.showForm = false;
    this.editingId = null;
    this.success = message;
    this.cdr.markForCheck();
    this.load();
  }

  private saveFailed(err: any): void {
    this.saving = false;
    this.error = err?.error?.message || 'Failed to save candidate';
    this.cdr.markForCheck();
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.api.delete(`/recruitment/talent-pool/${this.deleteTarget.id}`).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Removed from pool'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.deleteTarget = null; this.cdr.markForCheck(); },
    });
  }

  goToPage(p: number): void { this.page = p; this.load(); }

  skillList(c: PoolCandidate): string[] {
    return (c.skills || '').split(',').map((s) => s.trim()).filter(Boolean);
  }

  reasonLabel(r: string): string {
    return r.replaceAll('_', ' ');
  }

  reasonBadge(r: string): string {
    return {
      DECLINED_OFFER: 'badge-soft-warning', NO_VACANCY: 'badge-soft-primary', FUTURE_FIT: 'badge-soft-success',
      WITHDREW: 'text-bg-light border', REFERRAL: 'badge-soft-success', OTHER: 'text-bg-light border',
    }[r] || 'text-bg-light';
  }

  initials(c: PoolCandidate): string {
    const parts = c.name.trim().split(/\s+/);
    return ((parts[0]?.[0] || '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
  }
}
