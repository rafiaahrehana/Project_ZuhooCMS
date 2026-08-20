import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { Lead, LeadStatus, LOST_REASONS, Opportunity, PipelineSummary } from '../../models/crm.model';
import { OpportunityService } from '../../services/opportunity.service';
import { LeadService } from '../../services/lead.service';
import { Loader } from '../../../../shared/components/loader/loader';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
// Validated categorical/status slots (see dataviz skill palette) - fixed per
// series, never reassigned by data. Won/Lost use the reserved status colors
// since they represent an outcome's state, not an arbitrary category.
const STAGE_HUE = '#7d55fa';
const STATUS_GOOD = '#10b981';
const STATUS_CRITICAL = '#ef4444';
const LEAD_STATUS_COLORS: Record<LeadStatus, string> = {
  NEW: '#3b82f6',
  CONTACTED: '#f59e0b',
  QUALIFIED: '#10b981',
  DISQUALIFIED: '#ef4444',
};

export interface SalesPerformanceRow {
  ownerId: number | null;
  ownerName: string;
  totalOpportunities: number;
  wonCount: number;
  lostCount: number;
  wonValue: number;
  winRate: number;
}

@Component({
  selector: 'app-pipeline-reports',
  imports: [BosCurrencyPipe, CommonModule, RouterLink, BaseChartDirective, Loader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './pipeline-reports.html',
})
export class PipelineReports implements OnInit {
  loading = false;
  error = '';

  summary?: PipelineSummary;
  wonDeals: Opportunity[] = [];
  lostDeals: Opportunity[] = [];
  leads: Lead[] = [];
  allOpportunities: Opportunity[] = [];
  salesPerformance: SalesPerformanceRow[] = [];

  stageChartData?: ChartConfiguration<'bar'>['data'];
  stageChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: { x: { beginAtZero: true } },
  };

  trendChartData?: ChartConfiguration<'bar'>['data'];
  trendChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
    scales: { y: { beginAtZero: true } },
  };

  revenueChartData?: ChartConfiguration<'bar'>['data'];
  revenueChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } },
  };

  leadStatusChartData?: ChartConfiguration<'doughnut'>['data'];
  leadStatusChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'right' } },
  };

  constructor(
    private opportunityService: OpportunityService,
    private leadService: LeadService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    forkJoin({
      summary: this.opportunityService.pipelineSummary(),
      won: this.opportunityService.list(0, 500, { stage: 'WON' }),
      lost: this.opportunityService.list(0, 500, { stage: 'LOST' }),
      allOpportunities: this.opportunityService.list(0, 1000),
      leads: this.leadService.list(0, 1000),
    }).subscribe({
      next: ({ summary, won, lost, allOpportunities, leads }) => {
        this.summary = summary;
        this.wonDeals = won.content;
        this.lostDeals = lost.content;
        this.allOpportunities = allOpportunities.content;
        this.leads = leads.content;
        this.buildStageChart(summary);
        this.buildTrendChart();
        this.buildRevenueChart();
        this.buildLeadStatusChart();
        this.buildSalesPerformance();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load pipeline reports';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  // ── Weighted forecast ─────────────────────────────────────
  /**
   * Open pipeline grouped by expected close month: the raw sum and the
   * probability-weighted sum side by side. The weighted figure is the honest
   * one - five deals at 15% are not five deals - and "No close date" is its
   * own row rather than being dropped, because deals nobody dated are a
   * hygiene problem the manager should see.
   */
  get forecast(): { label: string; count: number; amount: number; weighted: number }[] {
    const open = this.allOpportunities.filter((o) => o.stage !== 'WON' && o.stage !== 'LOST');
    const buckets = new Map<string, { label: string; order: number; count: number; amount: number; weighted: number }>();

    for (const o of open) {
      let key = 'none';
      let label = 'No close date';
      let order = Number.MAX_SAFE_INTEGER;
      if (o.expectedCloseDate) {
        const d = new Date(o.expectedCloseDate);
        key = `${d.getFullYear()}-${d.getMonth()}`;
        label = d.toLocaleString('en-US', { month: 'short', year: 'numeric' });
        order = d.getFullYear() * 12 + d.getMonth();
      }
      const b = buckets.get(key) ?? { label, order, count: 0, amount: 0, weighted: 0 };
      b.count++;
      b.amount += o.amount || 0;
      b.weighted += (o.amount || 0) * ((o.probability ?? 0) / 100);
      buckets.set(key, b);
    }

    return [...buckets.values()]
      .sort((a, b) => a.order - b.order)
      .map(({ label, count, amount, weighted }) => ({ label, count, amount, weighted: Math.round(weighted) }));
  }

  get forecastTotals(): { amount: number; weighted: number } {
    return this.forecast.reduce(
      (acc, r) => ({ amount: acc.amount + r.amount, weighted: acc.weighted + r.weighted }),
      { amount: 0, weighted: 0 },
    );
  }

  // ── Loss reasons ──────────────────────────────────────────
  /**
   * Lost value by picklist code, largest first. Rows closed before the
   * picklist existed have only free text and are grouped as "Not recorded" -
   * shown, not hidden, so the report is honest about its own coverage.
   */
  get lossReasons(): { label: string; count: number; value: number; pct: number }[] {
    if (!this.lostDeals.length) return [];
    const labels = new Map<string, string>(LOST_REASONS.map((r) => [r.value, r.label]));
    const byCode = new Map<string, { count: number; value: number }>();
    for (const d of this.lostDeals) {
      const key = d.lostReasonCode ?? 'UNRECORDED';
      const b = byCode.get(key) ?? { count: 0, value: 0 };
      b.count++;
      b.value += d.amount || 0;
      byCode.set(key, b);
    }
    const total = this.lostDeals.length;
    return [...byCode.entries()]
      .map(([code, b]) => ({
        label: labels.get(code) ?? 'Not recorded',
        count: b.count,
        value: b.value,
        pct: Math.round((b.count / total) * 100),
      }))
      .sort((a, b) => b.count - a.count);
  }

  get leadConversionRate(): number {
    return this.leads.length > 0
      ? Math.round((this.leads.filter((l) => l.converted).length / this.leads.length) * 100)
      : 0;
  }

  get convertedLeadsCount(): number {
    return this.leads.filter((l) => l.converted).length;
  }

  private buildLeadStatusChart(): void {
    const statuses: LeadStatus[] = ['NEW', 'CONTACTED', 'QUALIFIED', 'DISQUALIFIED'];
    this.leadStatusChartData = {
      labels: statuses.map((s) => this.stageLabel(s)),
      datasets: [{
        data: statuses.map((s) => this.leads.filter((l) => l.status === s).length),
        backgroundColor: statuses.map((s) => LEAD_STATUS_COLORS[s]),
      }],
    };
  }

  // Sums actual won amount per calendar month (vs. the trend chart's deal count),
  // bucketed the same way as buildTrendChart for a consistent last-6-months window.
  private buildRevenueChart(): void {
    const months: { key: string; label: string }[] = [];
    const now = new Date();
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      months.push({ key: `${d.getFullYear()}-${d.getMonth()}`, label: d.toLocaleString('en-US', { month: 'short' }) });
    }
    const revenueByMonth = months.map(({ key }) =>
      this.wonDeals
        .filter((d) => {
          if (!d.actualCloseDate) return false;
          const cd = new Date(d.actualCloseDate);
          return `${cd.getFullYear()}-${cd.getMonth()}` === key;
        })
        .reduce((sum, d) => sum + (d.amount || 0), 0),
    );
    this.revenueChartData = {
      labels: months.map((m) => m.label),
      datasets: [{ data: revenueByMonth, backgroundColor: STATUS_GOOD, borderRadius: 4, maxBarThickness: 40 }],
    };
  }

  private buildSalesPerformance(): void {
    const byOwner = new Map<string, SalesPerformanceRow>();
    for (const o of this.allOpportunities) {
      const key = o.ownerId != null ? String(o.ownerId) : 'unassigned';
      if (!byOwner.has(key)) {
        byOwner.set(key, {
          ownerId: o.ownerId ?? null,
          ownerName: o.ownerName || 'Unassigned',
          totalOpportunities: 0, wonCount: 0, lostCount: 0, wonValue: 0, winRate: 0,
        });
      }
      const row = byOwner.get(key)!;
      row.totalOpportunities++;
      if (o.stage === 'WON') { row.wonCount++; row.wonValue += o.amount || 0; }
      if (o.stage === 'LOST') row.lostCount++;
    }
    this.salesPerformance = Array.from(byOwner.values())
      .map((row) => ({ ...row, winRate: row.wonCount + row.lostCount > 0 ? Math.round((row.wonCount / (row.wonCount + row.lostCount)) * 100) : 0 }))
      .sort((a, b) => b.wonValue - a.wonValue);
  }

  get wonAmount(): number {
    return this.wonDeals.reduce((sum, d) => sum + (d.amount || 0), 0);
  }

  get lostAmount(): number {
    return this.lostDeals.reduce((sum, d) => sum + (d.amount || 0), 0);
  }

  get winRate(): number {
    const total = this.wonDeals.length + this.lostDeals.length;
    return total > 0 ? Math.round((this.wonDeals.length / total) * 100) : 0;
  }

  private buildStageChart(summary: PipelineSummary): void {
    this.stageChartData = {
      labels: summary.stages.map((s) => this.stageLabel(s.stage)),
      datasets: [{
        data: summary.stages.map((s) => s.totalAmount),
        backgroundColor: STAGE_HUE,
        borderRadius: 4,
        maxBarThickness: 28,
      }],
    };
  }

  // Buckets closed deals into the last 6 calendar months by their close date,
  // so "trend" reflects when a deal was actually won/lost, not just a total.
  private buildTrendChart(): void {
    const months: { key: string; label: string }[] = [];
    const now = new Date();
    for (let i = 5; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      months.push({ key: `${d.getFullYear()}-${d.getMonth()}`, label: d.toLocaleString('en-US', { month: 'short' }) });
    }
    const bucket = (deals: Opportunity[]): number[] =>
      months.map(({ key }) =>
        deals.filter((d) => {
          if (!d.actualCloseDate) return false;
          const cd = new Date(d.actualCloseDate);
          return `${cd.getFullYear()}-${cd.getMonth()}` === key;
        }).length,
      );

    this.trendChartData = {
      labels: months.map((m) => m.label),
      datasets: [
        { label: 'Won', data: bucket(this.wonDeals), backgroundColor: STATUS_GOOD, borderRadius: 3 },
        { label: 'Lost', data: bucket(this.lostDeals), backgroundColor: STATUS_CRITICAL, borderRadius: 3 },
      ],
    };
  }

  stageLabel(stage: string): string {
    return stage.replace(/_/g, ' ');
  }
}
