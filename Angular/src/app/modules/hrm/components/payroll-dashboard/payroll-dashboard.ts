import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../../core/services/api.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { StatCard } from '../../../../shared/components/stat-card/stat-card';
import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';

interface TrendPoint { month: number; year: number; netPaid: number; }

interface DashboardView {
  month: number;
  year: number;
  totalEmployees: number;
  payrollCount: number;
  employeesPaid: number;
  pendingCount: number;
  totalGross: number;
  totalNet: number;
  totalDeductions: number;
  runStatus?: string;
  runNumber?: string;
  nextPayDate?: string;
  trend: TrendPoint[];
}

@Component({
  selector: 'app-payroll-dashboard',
  imports: [CommonModule, FormsModule, RouterLink, Loader, StatCard, BosCurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payroll-dashboard.html',
})
export class PayrollDashboard implements OnInit {
  view?: DashboardView;
  loading = false;
  error = '';

  month = new Date().getMonth() + 1;
  year = new Date().getFullYear();
  readonly months = ['January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'];

  get years(): number[] {
    const current = new Date().getFullYear();
    return [0, 1, 2, 3].map((o) => current - o);
  }

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.api.get<DashboardView>('/hr/payroll-dashboard', { month: this.month, year: this.year }).subscribe({
      next: (v) => { this.view = v; this.loading = false; this.cdr.markForCheck(); },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load payroll dashboard';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  monthShort(m: number): string {
    return this.months[m - 1]?.slice(0, 3) || '';
  }

  /** Bar height 8-100% of the largest month, so a flat trend still shows bars. */
  barHeight(p: TrendPoint): number {
    const max = Math.max(...(this.view?.trend || []).map((t) => t.netPaid || 0));
    if (!max) return 8;
    return Math.max(8, Math.round((p.netPaid / max) * 100));
  }

  runBadge(status?: string): string {
    return {
      DRAFT: 'text-bg-secondary', CALCULATED: 'text-bg-info', PENDING_APPROVAL: 'text-bg-warning',
      APPROVED: 'text-bg-primary', PAID: 'text-bg-success', REJECTED: 'text-bg-danger', CANCELLED: 'text-bg-light border',
    }[status || ''] || 'text-bg-light border';
  }
}
