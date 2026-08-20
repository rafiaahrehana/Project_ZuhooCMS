import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../../core/services/api.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';

export interface MonthPoint {
  month: number; year: number; label: string;
  revenue: number; expenses: number;
}

export interface BudgetLine {
  category: string; budgeted: number; spent: number; usedPercent: number;
}

export interface InvoiceLine {
  id: number; invoiceNumber: string; clientName?: string;
  dueDate?: string; totalAmount: number; balanceAmount: number;
  status: string; overdue: boolean;
}

export interface CategorySlice { category: string; amount: number; percent: number; }

export interface FinanceDashboardData {
  payMonth: number; payYear: number;
  totalRevenue: number; totalExpenses: number; netProfit: number;
  cashCollected: number; outstanding: number; overdue: number;
  payables: number; payablesOverdue: number; payrollCost: number;
  revenueChangePercent: number | null;
  expenseChangePercent: number | null;
  trend: MonthPoint[];
  budgets: BudgetLine[];
  recentInvoices: InvoiceLine[];
  expenseByCategory: CategorySlice[];
}

@Component({
  selector: 'app-finance-dashboard',
  imports: [CommonModule, FormsModule, RouterLink, Loader, BosCurrencyPipe],
  templateUrl: './finance-dashboard.html',
  styleUrl: './finance-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FinanceDashboard implements OnInit {
  data?: FinanceDashboardData;
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

  get years(): number[] {
    const current = new Date().getFullYear();
    return [0, 1, 2, 3, 4].map((o) => current - o);
  }

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    this.api.get<FinanceDashboardData>('/finance/dashboard', { month: this.month, year: this.year }).subscribe({
      next: (d) => { this.data = d; this.loading = false; this.cdr.markForCheck(); },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load the finance dashboard';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * Bar height as a percentage of the tallest value in the series.
   *
   * Drawn as plain divs rather than pulling in a chart library for one small
   * visual - the same reasoning as the HR dashboard's donut.
   */
  barHeight(value: number): number {
    const max = this.trendMax;
    if (max <= 0) return 0;
    return Math.max(2, Math.round((value / max) * 100));
  }

  get trendMax(): number {
    const points = this.data?.trend || [];
    return points.reduce((m, p) => Math.max(m, p.revenue, p.expenses), 0);
  }

  /**
   * Colour a budget bar by how much is consumed, so the colour carries meaning
   * rather than being decorative: over budget is danger, close to it warns.
   */
  budgetClass(used: number): string {
    if (used >= 100) return 'bg-danger';
    if (used >= 85) return 'bg-warning';
    return 'bg-success';
  }

  /** Up is good for revenue, bad for expenses - so the caller says which. */
  changeClass(change: number | null, upIsGood: boolean): string {
    if (change == null || change === 0) return 'text-muted';
    const up = change > 0;
    return up === upIsGood ? 'text-success' : 'text-danger';
  }

  changeIcon(change: number | null): string {
    if (change == null || change === 0) return 'bi-dash';
    return change > 0 ? 'bi-arrow-up' : 'bi-arrow-down';
  }

  statusClass(status: string): string {
    switch (status) {
      case 'PAID': return 'is-done';
      case 'PARTIALLY_PAID': return 'is-open';
      case 'OVERDUE': return 'is-failed';
      case 'CANCELLED':
      case 'VOIDED': return 'is-muted';
      default: return 'is-pending';
    }
  }
}
