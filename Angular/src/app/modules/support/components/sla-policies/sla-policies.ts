import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { SLAPolicy, SLAPolicyRequest } from '../../models/support.model';
import { SLAPolicyService } from '../../services/sla-policy.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-sla-policies',
  imports: [CommonModule, FormsModule, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sla-policies.html',
})
export class SlaPolicies implements OnInit {
  policies: SLAPolicy[] = [];
  loading = false;
  error = '';
  success = '';
  showForm = false;
  form: SLAPolicyRequest = this.emptyForm();
  editingId: number | null = null;
  deleteTarget: SLAPolicy | null = null;
  canManageSla = false;

  priorities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'];

  constructor(
    private slaService: SLAPolicyService,
    public auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const roles = this.auth.getCurrentUser()?.roles ?? [];
    this.canManageSla = roles.some(r => r === 'SUPER_ADMIN' || r === 'SYSTEM_ADMIN');
    this.load();
  }

  emptyForm(): SLAPolicyRequest {
    return {
      policyName: '',
      description: '',
      applicablePriority: 'MEDIUM',
      firstResponseTimeHours: 1,
      resolutionTimeHours: 8,
      businessHoursOnly: false,
      active: true,
      notes: '',
    };
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.slaService.list(0, 100).subscribe({
      next: (res) => {
        this.policies = res.content;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load SLA policies';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form = this.emptyForm();
    this.showForm = true;
  }

  openEdit(p: SLAPolicy): void {
    this.editingId = p.id;
    this.form = {
      policyName: p.policyName,
      description: p.description,
      applicablePriority: p.applicablePriority,
      firstResponseTimeHours: p.firstResponseTimeHours,
      resolutionTimeHours: p.resolutionTimeHours,
      businessHoursOnly: p.businessHoursOnly,
      active: p.active,
      notes: p.notes,
    };
    this.showForm = true;
  }

  save(): void {
    const op = this.editingId
      ? this.slaService.update(this.editingId, this.form)
      : this.slaService.create(this.form);
    op.subscribe({
      next: () => {
        this.showForm = false;
        this.success = this.editingId ? 'Policy updated' : 'Policy created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save policy'; this.cdr.markForCheck(); },
    });
  }

  toggleActive(p: SLAPolicy): void {
    this.slaService.updateStatus(p.id, !p.active).subscribe({
      next: () => this.load(),
      error: (err) => { this.error = err?.error?.message || 'Failed to update status'; this.cdr.markForCheck(); },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.slaService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Policy deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Cannot delete policy';
        this.cdr.markForCheck();
      },
    });
  }

  priorityClass(p: string): string {
    return (
      { CRITICAL: 'text-bg-danger', HIGH: 'text-bg-warning', MEDIUM: 'text-bg-info', LOW: 'text-bg-light' }[p] ||
      'text-bg-secondary'
    );
  }
}
