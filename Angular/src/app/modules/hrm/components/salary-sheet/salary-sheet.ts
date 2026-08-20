import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../../core/services/api.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

export interface SalarySheetRow {
  employeeId: number;
  employeeNumber: string;
  employeeName: string;
  position?: string;
  basic: number;
  houseRent: number;
  medical: number;
  transport: number;
  food: number;
  special: number;
  overtimeHours: number;
  overtimePayment: number;
  grossEarnings: number;
  absentDays: number;
  absentDeduction: number;
  tax: number;
  providentFund: number;
  totalDeductions: number;
  netPayable: number;
  note?: string;
  payrollId?: number;
  paymentStatus?: string;
  paymentMethod?: string;
  department?: string;
  otherEarnings?: number;
  otherDeductions?: number;
  bonus?: number;
  /** PAYROLL = actuals from the payroll register; PROJECTED = live estimate. */
  source?: string;
}

export interface SalarySheetData {
  payMonth: number;
  payYear: number;
  perDayBasis: string;
  perDayDivisor: number;
  overtimeEnabled: boolean;
  overtimeMultiplier: number;
  rows: SalarySheetRow[];
  totalBasic: number;
  totalHouseRent: number;
  totalMedical: number;
  totalTransport: number;
  totalFood: number;
  totalSpecial: number;
  totalOvertimeHours: number;
  totalOvertimePayment: number;
  totalBonus: number;
  totalOtherEarnings: number;
  totalOtherDeductions: number;
  totalGrossEarnings: number;
  totalAbsentDays: number;
  totalAbsentDeduction: number;
  totalTax: number;
  totalProvidentFund: number;
  totalDeductions: number;
  totalNetPayable: number;
}

@Component({
  selector: 'app-salary-sheet',
  imports: [CommonModule, FormsModule, Loader, EmptyState],
  templateUrl: './salary-sheet.html',
  styleUrl: './salary-sheet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SalarySheet implements OnInit {
  sheet?: SalarySheetData;
  loading = false;
  error = '';

  month = new Date().getMonth() + 1;
  year = new Date().getFullYear();

  readonly months = [
    { value: 1, label: 'January' }, { value: 2, label: 'February' }, { value: 3, label: 'March' },
    { value: 4, label: 'April' }, { value: 5, label: 'May' }, { value: 6, label: 'June' },
    { value: 7, label: 'July' }, { value: 8, label: 'August' }, { value: 9, label: 'September' },
    { value: 10, label: 'October' }, { value: 11, label: 'November' }, { value: 12, label: 'December' },
  ];

  /** Current year and the four before it - payroll is rarely restated further back. */
  get years(): number[] {
    const current = new Date().getFullYear();
    return [0, 1, 2, 3, 4].map((offset) => current - offset);
  }

  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    this.api.get<SalarySheetData>('/hr/salary-sheet', { month: this.month, year: this.year }).subscribe({
      next: (data) => { this.sheet = data; this.loading = false; this.cdr.markForCheck(); },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load the salary sheet';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  /** How the per-day rate was reached, spelled out under the heading. */
  get basisLabel(): string {
    if (!this.sheet) return '';
    const readable: Record<string, string> = {
      CALENDAR_DAYS: 'calendar days',
      FIXED_30: 'fixed 30-day month',
      FIXED_26: 'fixed 26-day month',
      ACTUAL_WORKING_DAYS: 'actual working days',
    };
    const basis = readable[this.sheet.perDayBasis] || this.sheet.perDayBasis;
    return `${basis} · ÷ ${this.sheet.perDayDivisor} per day`;
  }

  /** Rows with no salary structure are the ones an admin needs to act on. */
  get unconfiguredCount(): number {
    return (this.sheet?.rows || []).filter((r) => !!r.note).length;
  }

  trackRow(_: number, row: SalarySheetRow): number {
    return row.employeeId;
  }

  // ── Search & filters ──────────────────────────────────────
  search = '';
  filterDepartment = '';
  filterStatus = '';

  get departments(): string[] {
    return [...new Set((this.sheet?.rows || []).map((r) => r.department).filter(Boolean))] as string[];
  }

  get visibleRows(): SalarySheetRow[] {
    let rows = this.sheet?.rows || [];
    const term = this.search.trim().toLowerCase();
    if (term) {
      rows = rows.filter((r) =>
        (r.employeeName || '').toLowerCase().includes(term)
        || (r.employeeNumber || '').toLowerCase().includes(term)
        || (r.position || '').toLowerCase().includes(term),
      );
    }
    if (this.filterDepartment) rows = rows.filter((r) => r.department === this.filterDepartment);
    if (this.filterStatus === 'PAID') rows = rows.filter((r) => r.paymentStatus === 'PAID');
    if (this.filterStatus === 'PENDING') rows = rows.filter((r) => r.paymentStatus !== 'PAID');
    return rows;
  }

  // ── Summary cards (spec: employees / gross / deductions / net / paid / pending) ──
  get paidCount(): number {
    return (this.sheet?.rows || []).filter((r) => r.paymentStatus === 'PAID').length;
  }
  get pendingCount(): number {
    return (this.sheet?.rows || []).length - this.paidCount;
  }

  statusBadge(status?: string): string {
    return {
      PAID: 'text-bg-success', APPROVED: 'text-bg-primary', DRAFT: 'text-bg-secondary',
      CANCELLED: 'text-bg-danger',
    }[status || ''] || 'text-bg-light border';
  }

  /**
   * Column totals for whatever is currently on screen.
   *
   * When a search is active the response's own totals are the wrong answer -
   * they cover the whole company, so a filtered view would show three rows
   * under a total for thirty. Totals are recomputed from the visible rows and
   * the footer says so.
   */
  get totals(): Record<string, number> {
    if (!this.filtering && this.sheet) {
      return {
        basic: this.sheet.totalBasic,
        houseRent: this.sheet.totalHouseRent,
        medical: this.sheet.totalMedical,
        transport: this.sheet.totalTransport,
        food: this.sheet.totalFood,
        special: this.sheet.totalSpecial,
        overtime: this.sheet.totalOvertimePayment,
        bonus: this.sheet.totalBonus || 0,
        otherEarnings: this.sheet.totalOtherEarnings || 0,
        gross: this.sheet.totalGrossEarnings,
        absentDeduction: this.sheet.totalAbsentDeduction,
        tax: this.sheet.totalTax,
        providentFund: this.sheet.totalProvidentFund,
        otherDeductions: this.sheet.totalOtherDeductions || 0,
        deductions: this.sheet.totalDeductions,
        net: this.sheet.totalNetPayable,
      };
    }
    const sum = (pick: (r: SalarySheetRow) => number) =>
      this.visibleRows.reduce((acc, r) => acc + (pick(r) || 0), 0);
    return {
      basic: sum((r) => r.basic),
      houseRent: sum((r) => r.houseRent),
      medical: sum((r) => r.medical),
      transport: sum((r) => r.transport),
      food: sum((r) => r.food),
      special: sum((r) => r.special),
      overtime: sum((r) => r.overtimePayment),
      bonus: sum((r) => r.bonus || 0),
      otherEarnings: sum((r) => r.otherEarnings || 0),
      gross: sum((r) => r.grossEarnings),
      absentDeduction: sum((r) => r.absentDeduction),
      tax: sum((r) => r.tax),
      providentFund: sum((r) => r.providentFund),
      otherDeductions: sum((r) => r.otherDeductions || 0),
      deductions: sum((r) => r.totalDeductions),
      net: sum((r) => r.netPayable),
    };
  }

  get filtering(): boolean {
    return this.search.trim().length > 0 || !!this.filterDepartment || !!this.filterStatus;
  }

  /** Food, special, overtime, bonus and extra components share one column. */
  others(row: SalarySheetRow): number {
    return (row.food || 0) + (row.special || 0) + (row.overtimePayment || 0)
      + (row.otherEarnings || 0) + (row.bonus || 0);
  }

  /** How many rows restate the payroll register vs live projections. */
  get actualsCount(): number {
    return (this.sheet?.rows || []).filter((r) => r.source === 'PAYROLL').length;
  }

  /**
   * Excel-friendly CSV of the whole month (never the filtered view - a salary
   * sheet is a month-end record). Excel opens it directly.
   */
  exportCsv(): void {
    const rows = this.sheet?.rows || [];
    if (!rows.length) return;
    const esc = (v: unknown) => `"${String(v ?? '').replaceAll('"', '""')}"`;
    const header = ['Employee ID', 'Employee Name', 'Department', 'Designation', 'Basic', 'House Rent',
      'Medical', 'Transport', 'Food', 'Special', 'Overtime', 'Other Earnings', 'Gross',
      'Absent Deduction', 'Tax', 'PF', 'Other Deductions', 'Total Deductions', 'Net Salary',
      'Payment Method', 'Payment Status'];
    const lines = rows.map((r) => [
      r.employeeNumber, r.employeeName, r.department || '', r.position || '',
      r.basic, r.houseRent, r.medical, r.transport, r.food, r.special,
      r.overtimePayment, r.otherEarnings || 0, r.grossEarnings,
      r.absentDeduction, r.tax, r.providentFund, r.otherDeductions || 0,
      r.totalDeductions, r.netPayable, r.paymentMethod || '', r.paymentStatus || 'NOT GENERATED',
    ].map(esc).join(','));
    const csv = [header.map(esc).join(','), ...lines].join('\r\n');
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `salary-sheet-${this.year}-${String(this.month).padStart(2, '0')}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  initials(row: SalarySheetRow): string {
    const parts = (row.employeeName || '').trim().split(/\s+/);
    const first = parts[0]?.charAt(0) || '';
    const last = parts.length > 1 ? parts[parts.length - 1].charAt(0) : '';
    return (first + last).toUpperCase() || '?';
  }

  private static readonly ROW_ACCENTS = [
    '#0D9488', '#F59E0B', '#10B981', '#6366F1',
    '#8B5CF6', '#2563EB', '#E11D48', '#65A30D',
  ];

  /** Keyed on employee id so a person keeps their colour across months and filters. */
  accentFor(row: SalarySheetRow): string {
    return SalarySheet.ROW_ACCENTS[(row.employeeId || 0) % SalarySheet.ROW_ACCENTS.length];
  }

  // ── Export ────────────────────────────────────────────────
  exporting = false;

  /**
   * Exports the whole month, not the filtered view. A salary sheet is a
   * month-end record; handing someone a PDF that silently omitted the
   * employees a search box happened to be hiding would be worse than useless.
   */
  exportPdf(): void {
    if (this.exporting) return;
    this.exporting = true;
    this.error = '';
    this.cdr.markForCheck();

    this.api.getBlob(`/hr/salary-sheet/export?month=${this.month}&year=${this.year}`).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `salary-sheet-${this.year}-${String(this.month).padStart(2, '0')}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to export the salary sheet';
        this.exporting = false;
        this.cdr.markForCheck();
      },
    });
  }
}
