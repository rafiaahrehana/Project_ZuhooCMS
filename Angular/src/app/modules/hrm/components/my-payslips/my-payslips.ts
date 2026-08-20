import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Payroll } from '../../models/hrm.model';
import { PayrollService } from '../../services/payroll.service';
import { EmployeeService } from '../../services/employee.service';
import { PermissionService } from '../../../../core/services/permission.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

@Component({
  selector: 'app-my-payslips',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState],
  templateUrl: './my-payslips.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyPayslips implements OnInit {
  payslips: Payroll[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';

  /**
   * Whether this user may see the whole company's payslips.
   *
   * PAYROLL_VIEW is the marker the backend already uses to decide the same
   * thing, so the two cannot drift: an owner or HR manager gets every
   * employee's payslip, and everyone else gets only their own. There is no
   * toggle between the two - which set you see follows from what you may see.
   */
  readonly canViewAll: boolean;

  /** Period filter, used only in the company-wide view. */
  month = new Date().getMonth() + 1;
  year = new Date().getFullYear();
  readonly months = Array.from({ length: 12 }, (_, i) => i + 1);
  readonly years: number[];

  /** Free-text filter over employee name / number, company-wide view only. */
  search = '';

  downloadingId: number | null = null;

  /** Payslip open in the detail modal, mirroring the PDF's full breakdown. */
  viewing: Payroll | null = null;

  private employeeId: number | null = null;

  constructor(
    private payrollService: PayrollService,
    private employeeService: EmployeeService,
    private permissions: PermissionService,
    private cdr: ChangeDetectorRef,
  ) {
    this.canViewAll = this.permissions.hasPermission('PAYROLL_VIEW');
    const thisYear = new Date().getFullYear();
    this.years = [thisYear + 1, thisYear, thisYear - 1, thisYear - 2];
  }

  ngOnInit(): void {
    if (this.canViewAll) {
      this.load();
      return;
    }
    // Only the personal view needs to resolve which employee "me" is.
    this.loading = true;
    this.employeeService.getMyProfile().subscribe({
      next: (emp) => {
        this.employeeId = emp.id;
        this.load();
      },
      error: () => {
        this.error = 'Failed to load your profile';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';

    const request$ = this.canViewAll
      ? this.payrollService.listByPeriod(this.month, this.year, this.page)
      : this.employeeId
        ? this.payrollService.listForEmployee(this.employeeId, this.page)
        : null;

    if (!request$) {
      this.loading = false;
      return;
    }

    request$.subscribe({
      next: (res) => {
        this.payslips = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load payslips';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  /** Client-side name filter over the loaded page. */
  get visiblePayslips(): Payroll[] {
    const term = this.search.trim().toLowerCase();
    if (!term) return this.payslips;
    return this.payslips.filter((p) =>
      (p.employeeName || '').toLowerCase().includes(term)
      || String(p.payMonth).includes(term)
      || String(p.payYear).includes(term),
    );
  }

  onPeriodChange(): void {
    this.page = 0;
    this.load();
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  /**
   * A DRAFT has not been approved, so its figures can still change. Offering a
   * downloadable document for one would put a payslip in someone's hands that
   * does not match what they are eventually paid.
   */
  canDownload(p: Payroll): boolean {
    return p.status !== 'DRAFT' || this.canViewAll;
  }

  download(p: Payroll): void {
    if (this.downloadingId != null || !p.id) return;
    this.downloadingId = p.id;
    this.error = '';
    this.cdr.markForCheck();

    this.payrollService.payslipPdf(p.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `payslip-${p.payYear}-${String(p.payMonth).padStart(2, '0')}.pdf`;
        a.click();
        // Without this the blob is held for the life of the document.
        URL.revokeObjectURL(url);
        this.downloadingId = null;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Could not download that payslip. Please try again.';
        this.downloadingId = null;
        this.cdr.markForCheck();
      },
    });
  }

  /** Every earning on the slip, mirroring how the PDF totals its left column. */
  gross(p: Payroll): number {
    return (p.basicSalary || 0) + (p.houseRent || 0) + (p.medicalAllowance || 0)
      + (p.transportAllowance || 0) + (p.foodAllowance || 0) + (p.specialAllowance || 0)
      + (p.bonus || 0) + (p.billablePay || 0) + (p.overtimePay || 0) + (p.otherEarnings || 0);
  }

  /** Every deduction on the slip, mirroring the PDF's right column. */
  totalDeductions(p: Payroll): number {
    return (p.taxDeduction || 0) + (p.providentFundDeduction || 0) + (p.insuranceDeduction || 0)
      + (p.attendanceDeduction || 0) + (p.deductions || 0) + (p.otherDeductions || 0);
  }

  statusClass(status: string): string {
    return (
      {
        DRAFT: 'text-bg-secondary',
        APPROVED: 'text-bg-info',
        PAID: 'text-bg-success',
        CANCELLED: 'text-bg-danger',
      }[status] || 'text-bg-light'
    );
  }

  monthLabel(m: number): string {
    return new Date(2000, m - 1, 1).toLocaleString('en', { month: 'long' });
  }
}
