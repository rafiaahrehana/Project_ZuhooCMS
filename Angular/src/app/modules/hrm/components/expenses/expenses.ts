import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  HrExpense,
  HrExpenseRequest,
  HrExpenseStatus,
  HR_EXPENSE_STATUSES,
} from '../../models/hrm.model';
import { HrExpenseService } from '../../services/hr-expense.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-hr-expenses',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './expenses.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Expenses implements OnInit {
  // VARIABLES
  expenses: HrExpense[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  view: 'my' | 'all' = 'my';

  showForm = false;
  form: HrExpenseRequest = { title: '', amount: 0, expenseDate: '' };

  rejectTarget: HrExpense | null = null;
  rejectionReason = '';
  approveTarget: HrExpense | null = null;
  deleteTarget: HrExpense | null = null;

  statuses = HR_EXPENSE_STATUSES;

  constructor(private expenseService: HrExpenseService, private cdr: ChangeDetectorRef) {}

  // LIFECYCLE HOOKS
  ngOnInit(): void { this.load(); }

  // LOAD EXPENSES BASED ON CURRENT VIEW
  load(): void {
    this.loading = true;
    this.error = '';
    const req = this.view === 'my'
      ? this.expenseService.listMine(this.page)
      : this.expenseService.list(this.page);
    req.subscribe({
      next: (res) => { this.expenses = res.content; this.totalPages = res.totalPages; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load expenses'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // SWITCH BETWEEN MY EXPENSES AND ALL EXPENSES
  setView(v: 'my' | 'all'): void {
    if (this.view === v) return;
    this.view = v;
    this.page = 0;
    this.load();
  }

  // OPEN SUBMIT FORM
  openSubmit(): void {
    this.form = { title: '', amount: 0, expenseDate: '' };
    this.showForm = true;
  }

  // SUBMIT EXPENSE
  submit(): void {
    this.expenseService.submit(this.form).subscribe({
      next: () => { this.showForm = false; this.success = 'Expense submitted'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to submit'; this.cdr.markForCheck(); }
    });
  }

  // APPROVE EXPENSE
  doApprove(): void {
    if (!this.approveTarget) return;
    this.expenseService.approve(this.approveTarget.id).subscribe({
      next: () => { this.approveTarget = null; this.success = 'Expense approved'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to approve'; this.approveTarget = null; this.cdr.markForCheck(); }
    });
  }

  // REJECT EXPENSE
  doReject(): void {
    if (!this.rejectTarget || !this.rejectionReason) return;
    this.expenseService.reject(this.rejectTarget.id, this.rejectionReason).subscribe({
      next: () => { this.rejectTarget = null; this.rejectionReason = ''; this.success = 'Expense rejected'; this.cdr.markForCheck(); this.load(); },
      error: (err) => { this.error = err?.error?.message || 'Failed to reject'; this.rejectTarget = null; this.cdr.markForCheck(); }
    });
  }

  // DELETE EXPENSE
  doDelete(): void {
    if (!this.deleteTarget) return;
    this.expenseService.delete(this.deleteTarget.id).subscribe({
      next: () => { this.deleteTarget = null; this.success = 'Expense deleted'; this.cdr.markForCheck(); this.load(); },
      error: () => { this.deleteTarget = null; this.error = 'Cannot delete expense'; this.cdr.markForCheck(); }
    });
  }

  // PAGINATION
  goToPage(p: number): void { this.page = p; this.load(); }

  // STATUS BADGE CLASS
  statusClass(status: HrExpenseStatus): string {
    return {
      PENDING: 'text-bg-warning',
      APPROVED: 'text-bg-success',
      REJECTED: 'text-bg-danger',
      REIMBURSED: 'text-bg-primary',
    }[status] || 'text-bg-secondary';
  }
}
