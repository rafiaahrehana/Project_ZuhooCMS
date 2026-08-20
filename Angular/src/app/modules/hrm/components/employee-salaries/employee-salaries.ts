import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../../core/services/api.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
import { SalarySheetData, SalarySheetRow } from '../salary-sheet/salary-sheet';

/**
 * Per-employee view of the CURRENT salary package. Same numbers the salary
 * sheet computes for this month - presented per person, with the employees
 * who still have no structure surfaced instead of buried.
 */
@Component({
  selector: 'app-employee-salaries',
  imports: [CommonModule, FormsModule, RouterLink, Loader, EmptyState, BosCurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './employee-salaries.html',
})
export class EmployeeSalaries implements OnInit {
  sheet?: SalarySheetData;
  loading = false;
  error = '';
  search = '';

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    const now = new Date();
    this.loading = true;
    this.api.get<SalarySheetData>('/hr/salary-sheet', { month: now.getMonth() + 1, year: now.getFullYear() }).subscribe({
      next: (data) => { this.sheet = data; this.loading = false; this.cdr.markForCheck(); },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load employee salaries';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get rows(): SalarySheetRow[] {
    let rows = this.sheet?.rows || [];
    const term = this.search.trim().toLowerCase();
    if (term) {
      rows = rows.filter((r) =>
        (r.employeeName || '').toLowerCase().includes(term)
        || (r.employeeNumber || '').toLowerCase().includes(term)
        || (r.position || '').toLowerCase().includes(term));
    }
    return rows;
  }

  get unconfiguredCount(): number {
    return (this.sheet?.rows || []).filter((r) => !!r.note).length;
  }

  allowances(r: SalarySheetRow): number {
    return (r.houseRent || 0) + (r.medical || 0) + (r.transport || 0) + (r.food || 0) + (r.special || 0);
  }
}
