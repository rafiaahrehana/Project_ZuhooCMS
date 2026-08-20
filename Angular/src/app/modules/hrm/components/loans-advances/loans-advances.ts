import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../../core/services/api.service';
import { Employee } from '../../models/hrm.model';
import { EmployeeService } from '../../services/employee.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';

type LoanType = 'LOAN' | 'ADVANCE';
type LoanStatus = 'ACTIVE' | 'CLOSED' | 'CANCELLED';

interface LoanAdvance {
  id: number;
  employeeId: number;
  employeeName: string;
  type: LoanType;
  principalAmount: number;
  disbursedDate: string;
  monthlyInstallment: number;
  remainingBalance: number;
  status: LoanStatus;
  reason?: string;
  notes?: string;
  createdAt: string;
}

interface LoanRepayment {
  id: number;
  payrollId: number;
  payMonth: number;
  payYear: number;
  amount: number;
  paidDate: string;
  balanceAfter: number;
}

interface CreateLoanForm {
  employeeId: number | null;
  type: LoanType;
  principalAmount: number | null;
  disbursedDate: string;
  monthlyInstallment: number | null;
  reason: string;
  notes: string;
}

@Component({
  selector: 'app-loans-advances',
  imports: [CommonModule, FormsModule, Loader, EmptyState, ConfirmDialog, HasPermissionDirective, BosCurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './loans-advances.html',
})
export class LoansAdvances implements OnInit {
  loans: LoanAdvance[] = [];
  employees: Employee[] = [];
  loading = false;
  error = '';
  success = '';

  statusFilter: '' | LoanStatus = '';

  showForm = false;
  saving = false;
  form: CreateLoanForm = this.emptyForm();

  cancelTarget: LoanAdvance | null = null;

  historyTarget: LoanAdvance | null = null;
  history: LoanRepayment[] = [];
  historyLoading = false;

  readonly months = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  constructor(private api: ApiService, private employeeService: EmployeeService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  get filteredLoans(): LoanAdvance[] {
    return this.statusFilter ? this.loans.filter((l) => l.status === this.statusFilter) : this.loans;
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.api.get<LoanAdvance[]>('/hr/loans').subscribe({
      next: (res) => { this.loans = res; this.loading = false; this.cdr.markForCheck(); },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load loans and advances';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private emptyForm(): CreateLoanForm {
    return {
      employeeId: null,
      type: 'LOAN',
      principalAmount: null,
      disbursedDate: new Date().toISOString().slice(0, 10),
      monthlyInstallment: null,
      reason: '',
      notes: '',
    };
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.showForm = true;
    if (!this.employees.length) {
      this.employeeService.list(0, 200).subscribe({ next: (res) => { this.employees = res.content; this.cdr.markForCheck(); } });
    }
  }

  get canSave(): boolean {
    return !!this.form.employeeId && !!this.form.principalAmount && this.form.principalAmount > 0
      && !!this.form.monthlyInstallment && this.form.monthlyInstallment > 0 && !!this.form.disbursedDate;
  }

  save(): void {
    if (!this.canSave || this.saving) return;
    this.saving = true;
    this.cdr.markForCheck();
    this.api.post<LoanAdvance>('/hr/loans', this.form).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = 'Loan/advance created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to create loan/advance';
        this.cdr.markForCheck();
      },
    });
  }

  doCancel(): void {
    if (!this.cancelTarget) return;
    this.api.post(`/hr/loans/${this.cancelTarget.id}/cancel`, {}).subscribe({
      next: () => {
        this.cancelTarget = null;
        this.success = 'Loan/advance cancelled';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to cancel';
        this.cancelTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  openHistory(loan: LoanAdvance): void {
    this.historyTarget = loan;
    this.history = [];
    this.historyLoading = true;
    this.cdr.markForCheck();
    this.api.get<LoanRepayment[]>(`/hr/loans/${loan.id}/repayments`).subscribe({
      next: (res) => { this.history = res; this.historyLoading = false; this.cdr.markForCheck(); },
      error: () => { this.historyLoading = false; this.cdr.markForCheck(); },
    });
  }

  progressPercent(loan: LoanAdvance): number {
    if (!loan.principalAmount) return 0;
    const paid = loan.principalAmount - loan.remainingBalance;
    return Math.max(0, Math.min(100, Math.round((paid / loan.principalAmount) * 100)));
  }

  statusBadge(status: LoanStatus): string {
    return { ACTIVE: 'text-bg-success', CLOSED: 'text-bg-secondary', CANCELLED: 'text-bg-light border' }[status];
  }

  typeBadge(type: LoanType): string {
    return type === 'LOAN' ? 'badge-soft-info' : 'badge-soft-secondary';
  }
}
