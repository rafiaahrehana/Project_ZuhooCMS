import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { FiscalYearSummary } from '../../models/finance.model';
import { AccountingPeriodService } from '../../services/accounting-period.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { StatCard } from '../../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-fiscal-years',
  imports: [CommonModule, FormsModule, RouterLink, Loader, EmptyState, HasPermissionDirective, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './fiscal-years.html',
})
export class FiscalYears implements OnInit {
  years: FiscalYearSummary[] = [];
  loading = false;
  error = '';
  success = '';
  searchTerm = '';

  // "New Fiscal Year" modal state
  showCreate = false;
  newYear: number = new Date().getFullYear();
  creating = false;

  constructor(private periodService: AccountingPeriodService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.periodService.listFiscalYears().subscribe({
      next: (res) => {
        this.years = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load fiscal years';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  get filteredYears(): FiscalYearSummary[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) return this.years;
    return this.years.filter(
      (y) => (y.name || '').toLowerCase().includes(term) || String(y.fiscalYear).includes(term)
    );
  }

  get totalYears(): number {
    return this.years.length;
  }
  get activeCount(): number {
    return this.years.filter((y) => y.status === 'ACTIVE').length;
  }
  get draftCount(): number {
    return this.years.filter((y) => y.status === 'DRAFT').length;
  }
  get closedCount(): number {
    return this.years.filter((y) => y.status === 'CLOSED').length;
  }

  statusBadge(status: FiscalYearSummary['status']): string {
    switch (status) {
      case 'ACTIVE':
        return 'text-bg-success';
      case 'CLOSED':
        return 'text-bg-dark';
      default:
        return 'text-bg-secondary';
    }
  }

  openCreate(): void {
    const latest = this.years.reduce((max, y) => Math.max(max, y.fiscalYear), 0);
    this.newYear = latest ? latest + 1 : new Date().getFullYear();
    this.showCreate = true;
  }

  createYear(): void {
    if (this.creating || !this.newYear) return;
    this.creating = true;
    this.error = '';
    this.cdr.markForCheck();
    // listForYear() auto-generates the 12 monthly periods server-side on first call.
    this.periodService.listForYear(this.newYear).subscribe({
      next: () => {
        this.creating = false;
        this.showCreate = false;
        this.success = `Fiscal year ${this.newYear} created with its 12 accounting periods`;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to create fiscal year';
        this.creating = false;
        this.cdr.markForCheck();
      },
    });
  }
}
