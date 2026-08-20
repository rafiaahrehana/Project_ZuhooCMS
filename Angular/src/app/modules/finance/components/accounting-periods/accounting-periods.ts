import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AccountingPeriod } from '../../models/finance.model';
import { AccountingPeriodService } from '../../services/accounting-period.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { StatCard } from '../../../../shared/components/stat-card/stat-card';

@Component({
  selector: 'app-accounting-periods',
  imports: [CommonModule, FormsModule, RouterLink, Loader, ConfirmDialog, HasPermissionDirective, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './accounting-periods.html',
})
export class AccountingPeriods implements OnInit {
  fiscalYear = new Date().getFullYear();
  periods: AccountingPeriod[] = [];
  loading = false;
  error = '';
  success = '';

  closingId: number | null = null;
  closeYearConfirm = false;
  closingYear = false;

  // Today's date as a local ISO string (yyyy-MM-dd) for "current period" checks.
  private readonly todayIso: string = (() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  })();

  constructor(
    private periodService: AccountingPeriodService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const yearParam = Number(this.route.snapshot.queryParamMap.get('year'));
    if (Number.isInteger(yearParam) && yearParam > 0) this.fiscalYear = yearParam;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.periodService.listForYear(this.fiscalYear).subscribe({
      next: (res) => {
        this.periods = res;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load accounting periods';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  changeYear(delta: number): void {
    this.fiscalYear += delta;
    this.load();
  }

  get allClosed(): boolean {
    return this.periods.length === 12 && this.periods.every((p) => p.status === 'CLOSED');
  }

  get openCount(): number {
    return this.periods.filter((p) => p.status === 'OPEN').length;
  }

  get closedCount(): number {
    return this.periods.filter((p) => p.status === 'CLOSED').length;
  }

  /** The period whose date range contains today, if it belongs to the loaded year. */
  get currentPeriod(): AccountingPeriod | null {
    return this.periods.find((p) => this.isCurrent(p)) ?? null;
  }

  get currentLabel(): string {
    return this.currentPeriod?.label || '—';
  }

  isCurrent(period: AccountingPeriod): boolean {
    return period.startDate <= this.todayIso && period.endDate >= this.todayIso;
  }

  // Sequential-close guard mirrors the backend rule (close in date order) so most
  // invalid clicks never round-trip to the server at all.
  canClose(period: AccountingPeriod): boolean {
    if (period.status === 'CLOSED') return false;
    return !this.periods.some((p) => p.startDate < period.startDate && p.status === 'OPEN');
  }

  canReopen(period: AccountingPeriod): boolean {
    if (period.status !== 'CLOSED') return false;
    return !this.periods.some((p) => p.startDate > period.startDate && p.status === 'OPEN');
  }

  closePeriod(period: AccountingPeriod): void {
    if (!this.canClose(period) || this.closingId) return;
    this.closingId = period.id;
    this.error = '';
    this.cdr.markForCheck();
    this.periodService.closePeriod(period.id).subscribe({
      next: (updated) => {
        this.applyUpdate(updated);
        this.closingId = null;
        this.success = `${period.label} closed`;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to close period';
        this.closingId = null;
        this.cdr.markForCheck();
      },
    });
  }

  reopenPeriod(period: AccountingPeriod): void {
    if (!this.canReopen(period) || this.closingId) return;
    this.closingId = period.id;
    this.error = '';
    this.cdr.markForCheck();
    this.periodService.reopenPeriod(period.id).subscribe({
      next: (updated) => {
        this.applyUpdate(updated);
        this.closingId = null;
        this.success = `${period.label} reopened`;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to reopen period';
        this.closingId = null;
        this.cdr.markForCheck();
      },
    });
  }

  private applyUpdate(updated: AccountingPeriod): void {
    const i = this.periods.findIndex((p) => p.id === updated.id);
    if (i > -1) this.periods[i] = updated;
  }

  confirmCloseYear(): void {
    this.closingYear = true;
    this.error = '';
    this.cdr.markForCheck();
    this.periodService.closeFiscalYear(this.fiscalYear).subscribe({
      next: () => {
        this.closingYear = false;
        this.closeYearConfirm = false;
        this.success = `Fiscal year ${this.fiscalYear} closed - net income posted to Retained Earnings`;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.closingYear = false;
        this.closeYearConfirm = false;
        this.error = err?.error?.message || 'Failed to close fiscal year';
        this.cdr.markForCheck();
      },
    });
  }
}
