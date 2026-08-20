import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Expense, ChartOfAccount } from '../../models/finance.model';
import { ExpenseService } from '../../services/expense.service';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { CoaService } from '../../services/coa.service';
import { BudgetService } from '../../services/budget.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
import { SpeechInputService } from '../../../../shared/services/speech-input.service';
@Component({
  selector: 'app-expenses',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './expenses.html',
})
export class Expenses implements OnInit {
  expenses: Expense[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';
  showForm = false;
  form: any = {};
  /** Set while editing an existing PENDING expense - save() switches to update(). */
  editingId: number | null = null;
  /** Expense open in the full-detail modal. */
  viewing: Expense | null = null;
  /** For "on behalf of" attribution - empty (and hidden) when the user may not list employees. */
  employees: { id: number; firstName: string; lastName: string }[] = [];
  approvalTarget: Expense | null = null;
  approvalNotes = '';
  approvalAction: 'approve' | 'reject' = 'approve';
  deleteTarget: Expense | null = null;

  payTarget: Expense | null = null;
  payMethod = '';
  payReference = '';

  statuses = ['PENDING', 'APPROVED', 'REJECTED', 'PAID', 'CANCELLED'];

  // EXPENSE-type COA accounts for the "posts to" select (lazy-loaded on first form open)
  expenseAccounts: ChartOfAccount[] = [];
  // Existing budget category names, suggested via <datalist> so free-text entry here
  // doesn't drift from what a budget was actually set up for (typos silently break
  // the budget-vs-actual matching - see BudgetService.toResponse()).
  budgetCategories: string[] = [];

  constructor(
    private expenseService: ExpenseService,
    private coaService: CoaService,
    private budgetService: BudgetService,
    private employeeService: EmployeeService,
    private cdr: ChangeDetectorRef,
    private speechInput: SpeechInputService,
  ) {}

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
  }

  composeNotes = '';
  composing = false;
  composeError = '';

  openForm(): void {
    this.showForm = true;
    this.editingId = null;
    this.form = {};
    this.composeNotes = '';
    this.composeError = '';
    this.loadFormLookups();
  }

  composeWithAi(): void {
    const notes = this.composeNotes.trim();
    if (!notes || this.composing) return;
    this.composing = true;
    this.composeError = '';
    this.cdr.markForCheck();
    this.expenseService.composeEntry({
      vendorName: this.form.vendorName,
      amount: this.form.amount != null ? String(this.form.amount) : undefined,
      category: this.form.category,
      roughNotes: notes,
    }).subscribe({
      next: (res) => {
        this.form.title = res.title;
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

  /** Only PENDING expenses are editable - approval freezes the figures. */
  openEdit(e: Expense): void {
    this.showForm = true;
    this.editingId = e.id;
    this.form = {
      title: e.title,
      description: e.description,
      amount: e.amount,
      currency: e.currency,
      expenseDate: e.expenseDate,
      category: e.category,
      expenseAccountId: e.expenseAccountId,
      vendorName: e.vendorName,
      receiptUrl: e.receiptUrl,
      notes: e.notes,
      referenceNumber: e.referenceNumber,
    };
    this.loadFormLookups();
  }

  private loadFormLookups(): void {
    if (!this.employees.length) {
      this.employeeService.list(0, 500).subscribe({
        next: (res: any) => { this.employees = res.content || []; this.cdr.markForCheck(); },
        error: () => {},
      });
    }
    if (!this.expenseAccounts.length) {
      this.coaService.list(0, 200).subscribe({
        next: (res) => {
          this.expenseAccounts = res.content.filter((a) => a.type === 'EXPENSE' && a.active);
          this.cdr.markForCheck();
        },
        error: () => {},
      });
    }
    if (!this.budgetCategories.length) {
      this.budgetService.listCategories().subscribe({
        next: (res) => { this.budgetCategories = res; this.cdr.markForCheck(); },
        error: () => {},
      });
    }
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
        this.error = 'Failed to load expenses';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  save(): void {
    const op = this.editingId
      ? this.expenseService.update(this.editingId, this.form)
      : this.expenseService.create(this.form);
    op.subscribe({
      next: () => {
        this.success = this.editingId ? 'Expense updated' : 'Expense submitted';
        this.showForm = false;
        this.editingId = null;
        this.form = {};
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save'; this.cdr.markForCheck(); },
    });
  }

  openApproval(e: Expense, action: 'approve' | 'reject'): void {
    this.approvalTarget = e;
    this.approvalAction = action;
    this.approvalNotes = '';
  }

  budgetWarning = '';

  doApproval(): void {
    if (!this.approvalTarget) return;
    this.budgetWarning = '';
    if (this.approvalAction === 'approve') {
      this.expenseService.approve(this.approvalTarget.id, this.approvalNotes).subscribe({
        next: (res) => {
          this.approvalTarget = null;
          this.success = 'Expense approved';
          this.budgetWarning = res?.budgetWarning || '';
          this.cdr.markForCheck();
          this.load();
        },
        error: (err) => {
          this.error = err?.error?.message || 'Failed';
          this.approvalTarget = null;
          this.cdr.markForCheck();
        },
      });
      return;
    }
    this.expenseService.reject(this.approvalTarget.id, this.approvalNotes).subscribe({
      next: () => {
        this.approvalTarget = null;
        this.success = 'Expense rejected';
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
