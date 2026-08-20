import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LeaveRequest,
  LeaveRequestPayload,
  LeaveRequestStatus,
  LeaveBalance,
  LEAVE_TYPES,
  LEAVE_REQUEST_STATUSES,
} from '../../models/hrm.model';
import { LeaveService } from '../../services/leave.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { PermissionService } from '../../../../core/services/permission.service';

@Component({
  selector: 'app-leaves',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './leaves.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Leaves implements OnInit {
  // VARIABLES
  requests: LeaveRequest[] = [];
  balances: LeaveBalance[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  /**
   * Approvers see the whole company's queue; everyone else sees only their own
   * requests. There is no toggle between the two - which set you get follows
   * from whether you can act on other people's leave, so offering both views
   * would just be an empty tab for one group and a redundant one for the other.
   */
  readonly isApprover: boolean;
  view: 'my' | 'all';
  statusFilter: LeaveRequestStatus | '' = '';

  showForm = false;
  form: LeaveRequestPayload = { leaveType: 'ANNUAL', startDate: '', endDate: '' };

  reviewTarget: LeaveRequest | null = null;
  reviewAction: 'APPROVED' | 'REJECTED' = 'APPROVED';
  rejectionReason = '';
  cancelTarget: LeaveRequest | null = null;

  leaveTypes = LEAVE_TYPES;
  statuses = LEAVE_REQUEST_STATUSES;

  constructor(
    private leaveService: LeaveService,
    private permissions: PermissionService,
    private cdr: ChangeDetectorRef,
  ) {
    // Either code is enough: a reviewer who can only reject still needs the queue.
    this.isApprover = this.permissions.hasAnyPermission(['LEAVE_APPROVE', 'LEAVE_REJECT']);
    this.view = this.isApprover ? 'all' : 'my';
  }

  // LIFECYCLE HOOKS
  ngOnInit(): void {
    this.load();
    // Balances are the employee's own entitlement. An approver looking at the
    // company queue has no use for their personal counts on this screen.
    if (!this.isApprover) this.loadBalances();
  }

  // LOAD LEAVE REQUESTS BASED ON CURRENT VIEW
  load(): void {
    this.loading = true;
    this.error = '';
    const req = this.view === 'my'
      ? this.leaveService.listMine(this.page)
      : this.leaveService.list(this.page, 20, this.statusFilter || undefined);
    req.subscribe({
      next: (res) => { this.requests = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load leave requests'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // LOAD MY BALANCES FOR CURRENT YEAR
  loadBalances(): void {
    this.leaveService.myBalances().subscribe({
      next: (data) => { this.balances = data; this.cdr.markForCheck(); },
      error: () => { this.balances = []; this.cdr.markForCheck(); }
    });
  }

  // OPEN APPLY FORM
  openApply(): void {
    this.form = { leaveType: 'ANNUAL', startDate: '', endDate: '' };
    this.showForm = true;
  }

  // APPLY FOR LEAVE
  apply(): void {
    this.leaveService.apply(this.form).subscribe({
      next: () => { this.success = 'Leave request submitted'; this.showForm = false; this.cdr.markForCheck(); this.load(); this.loadBalances(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to submit leave request'; this.cdr.markForCheck(); }
    });
  }

  // OPEN REVIEW DIALOG
  openReview(r: LeaveRequest, action: 'APPROVED' | 'REJECTED'): void {
    this.reviewTarget = r;
    this.reviewAction = action;
    this.rejectionReason = '';
  }

  // SUBMIT REVIEW
  doReview(): void {
    if (!this.reviewTarget) return;
    this.leaveService.review(this.reviewTarget.id, {
      status: this.reviewAction,
      rejectionReason: this.reviewAction === 'REJECTED' ? this.rejectionReason : undefined,
    }).subscribe({
      next: () => { this.reviewTarget = null; this.success = 'Leave request reviewed'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to review'; this.reviewTarget = null; this.cdr.markForCheck(); }
    });
  }

  // CANCEL OWN REQUEST
  doCancel(): void {
    if (!this.cancelTarget) return;
    this.leaveService.cancel(this.cancelTarget.id).subscribe({
      next: () => { this.cancelTarget = null; this.success = 'Leave request cancelled'; this.cdr.markForCheck(); this.load(); this.loadBalances(); },
      error: (err) => { this.error = err?.error?.message || 'Cannot cancel'; this.cancelTarget = null; this.cdr.markForCheck(); }
    });
  }

  // PAGINATION
  goToPage(p: number): void { this.page = p; this.load(); }

  // STATUS BADGE CLASS
  statusClass(status: LeaveRequestStatus): string {
    return {
      PENDING: 'text-bg-warning',
      APPROVED: 'text-bg-success',
      REJECTED: 'text-bg-danger',
      CANCELLED: 'text-bg-secondary',
    }[status] || 'text-bg-secondary';
  }
}
