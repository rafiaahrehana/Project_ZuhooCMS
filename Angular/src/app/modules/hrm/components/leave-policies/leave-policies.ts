import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LeavePolicy,
  LeavePolicyRequest,
  LEAVE_TYPES,
  EMPLOYMENT_TYPES
} from '../../models/hrm.model';
import { LeavePolicyService } from '../../services/leave-policy.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-leave-policies',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './leave-policies.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeavePolicies implements OnInit {
  policies: LeavePolicy[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  isEdit = false;
  selectedId: number | null = null;
  form: LeavePolicyRequest = this.emptyForm();

  deleteTarget: LeavePolicy | null = null;

  leaveTypes = LEAVE_TYPES;
  employmentTypes = EMPLOYMENT_TYPES;

  draftRemoteAllowed = false;
  draftContext = '';
  /** The AI drafting form lives in a modal, opened from the header. */
  showDraftModal = false;

  openDraft(): void {
    this.showDraftModal = true;
    this.draftError = '';
  }
  draftDocument = '';
  drafting = false;
  draftError = '';

  constructor(private policyService: LeavePolicyService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.policyService.list(this.page, 20).subscribe({
      next: (res) => {
        this.policies = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load leave policies';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.isEdit = false;
    this.showForm = true;
  }

  openEdit(p: LeavePolicy): void {
    this.form = {
      leaveType: p.leaveType,
      employmentType: p.employmentType,
      annualEntitlement: p.annualEntitlement,
      maxCarryForward: p.maxCarryForward,
      maxConsecutiveDays: p.maxConsecutiveDays,
      requiresApproval: p.requiresApproval,
      canCarryForward: p.canCarryForward,
      paid: p.paid,
      applicableFromMonths: p.applicableFromMonths,
    };
    this.selectedId = p.id;
    this.isEdit = true;
    this.showForm = true;
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const payload = this.cleanPayload();
    const request = this.isEdit && this.selectedId
      ? this.policyService.update(this.selectedId, payload)
      : this.policyService.create(payload);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.isEdit ? 'Leave policy updated' : 'Leave policy created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save policy';
        this.cdr.markForCheck();
      }
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.policyService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Leave policy deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Failed to delete policy';
        this.cdr.markForCheck();
      }
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  draftWithAi(): void {
    if (this.drafting) return;
    this.drafting = true;
    this.draftError = '';
    this.cdr.markForCheck();
    this.policyService.draftWithAi(this.draftRemoteAllowed, this.draftContext.trim()).subscribe({
      next: (res) => {
        this.draftDocument = res.document;
        this.drafting = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.draftError = err?.error?.message || 'Failed to generate draft';
        this.drafting = false;
        this.cdr.markForCheck();
      },
    });
  }

  private emptyForm(): LeavePolicyRequest {
    return {
      leaveType: 'ANNUAL',
      employmentType: 'FULL_TIME',
      annualEntitlement: 14,
      maxCarryForward: 5,
      maxConsecutiveDays: 10,
      requiresApproval: true,
      canCarryForward: true,
      paid: true,
      applicableFromMonths: 0,
    };
  }

  private cleanPayload(): LeavePolicyRequest {
    const payload: any = { ...this.form };
    return payload;
  }
}
