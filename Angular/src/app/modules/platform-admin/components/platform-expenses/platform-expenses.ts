import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Expense } from '../../../finance/models/finance.model';
import { PlatformExpenseService } from '../../services/platform-expense.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-platform-expenses',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './platform-expenses.html',
})
export class PlatformExpenses implements OnInit {
  expenses: Expense[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';
  showForm = false;
  form: any = {};
  approvalTarget: Expense | null = null;
  approvalNotes = '';
  approvalAction: 'approve' | 'reject' = 'approve';
  deleteTarget: Expense | null = null;

  payTarget: Expense | null = null;
  payMethod = '';
  payReference = '';

  statuses = ['PENDING', 'APPROVED', 'REJECTED', 'PAID'];

  constructor(private expenseService: PlatformExpenseService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    const obs = this.statusFilter
      ? this.expenseService.listByStatus(this.statusFilter, this.page)
      : this.expenseService.list(this.page);
    obs.subscribe({
      next: (res) => {
        this.expenses = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load platform expenses';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  save(): void {
    this.expenseService.create(this.form).subscribe({
      next: () => {
        this.showForm = false;
        this.form = {};
        this.success = 'Expense submitted';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to submit'; this.cdr.markForCheck(); },
    });
  }

  openApproval(e: Expense, action: 'approve' | 'reject'): void {
    this.approvalTarget = e;
    this.approvalAction = action;
    this.approvalNotes = '';
  }

  doApproval(): void {
    if (!this.approvalTarget) return;
    const op =
      this.approvalAction === 'approve'
        ? this.expenseService.approve(this.approvalTarget.id, this.approvalNotes)
        : this.expenseService.reject(this.approvalTarget.id, this.approvalNotes);
    op.subscribe({
      next: () => {
        this.approvalTarget = null;
        this.success = 'Done';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed';
        this.approvalTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.expenseService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Cannot delete';
        this.cdr.markForCheck();
      },
    });
  }

  openPay(e: Expense): void {
    this.payTarget = e;
    this.payMethod = '';
    this.payReference = '';
  }

  doPay(): void {
    if (!this.payTarget) return;
    this.expenseService.markAsPaid(this.payTarget.id, this.payMethod || undefined, this.payReference || undefined).subscribe({
      next: () => {
        this.payTarget = null;
        this.success = 'Marked as paid';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to mark as paid';
        this.payTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
  statusClass(status: string): string {
    return (
      {
        PENDING: 'text-bg-warning',
        APPROVED: 'text-bg-success',
        REJECTED: 'text-bg-danger',
        PAID: 'text-bg-primary',
      }[status] || 'text-bg-secondary'
    );
  }
}
