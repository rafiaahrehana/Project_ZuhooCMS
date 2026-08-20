import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { RecruitmentKpiSummary } from '../../models/hrm.model';
import { RecruitmentKpiService } from '../../services/recruitment-kpi.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { StatCard } from '../../../../shared/components/stat-card/stat-card';

// Fixed per-series colors, never reassigned by data (see dataviz skill).
const FUNNEL_HUE = '#7d55fa';
const SOURCE_COLORS = ['#3b82f6', '#f59e0b', '#10b981', '#ef4444', '#7d55fa', '#06b6d4', '#ec4899', '#84cc16'];
const APPLICATIONS_HUE = '#3b82f6';
const HIRED_HUE = '#10b981';
/** Job/recruiter charts cap at this many bars, sorted by activity, so a company with many postings/recruiters stays readable - the full set is still in the table below. */
const CHART_ROW_LIMIT = 8;

@Component({
  selector: 'app-recruitment-reports',
  imports: [CommonModule, FormsModule, BaseChartDirective, Loader, StatCard],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './recruitment-reports.html',
})
export class RecruitmentReports implements OnInit {
  loading = false;
  error = '';
  summary?: RecruitmentKpiSummary;

  // Filters on when a candidate applied (JobApplication.createdAt) - all-time
  // until the user picks dates and clicks Apply, same "Generate"-gated
  // pattern as the Finance Reports page. openPositions/hiresThisMonth in the
  // response stay real-time regardless of this filter - see the backend.
  fromDate = '';
  toDate = '';
  /** Only narrows the Top Evaluated Candidates list below - see the backend for why. */
  minScore: number | null = null;
  /** What the currently-shown data was actually filtered by - may lag fromDate/toDate/minScore until Apply is clicked. */
  appliedFromDate = '';
  appliedToDate = '';
  appliedMinScore: number | null = null;

  funnelChartData?: ChartConfiguration<'bar'>['data'];
  funnelChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: { legend: { display: false } },
    scales: { x: { beginAtZero: true, ticks: { precision: 0 } } },
  };

  sourceChartData?: ChartConfiguration<'doughnut'>['data'];
  sourceChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'right' } },
  };

  jobChartData?: ChartConfiguration<'bar'>['data'];
  jobChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
    scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
  };

  recruiterChartData?: ChartConfiguration<'bar'>['data'];
  recruiterChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
    scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
  };

  constructor(private kpiService: RecruitmentKpiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.appliedFromDate = this.fromDate;
    this.appliedToDate = this.toDate;
    this.appliedMinScore = this.minScore;
    this.kpiService.getSummary(this.fromDate || undefined, this.toDate || undefined, this.minScore ?? undefined).subscribe({
      next: (summary) => {
        this.summary = summary;
        this.buildFunnelChart(summary);
        this.buildSourceChart(summary);
        this.buildJobChart(summary);
        this.buildRecruiterChart(summary);
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load recruitment KPIs';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  applyFilter(): void {
    this.load();
  }

  clearFilter(): void {
    this.fromDate = '';
    this.toDate = '';
    this.minScore = null;
    this.load();
  }

  private buildFunnelChart(summary: RecruitmentKpiSummary): void {
    this.funnelChartData = {
      labels: summary.funnel.map((f) => f.stage),
      datasets: [{ data: summary.funnel.map((f) => f.count), backgroundColor: FUNNEL_HUE, borderRadius: 4, maxBarThickness: 28 }],
    };
  }

  private buildSourceChart(summary: RecruitmentKpiSummary): void {
    this.sourceChartData = {
      labels: summary.sourceBreakdown.map((s) => this.sourceLabel(s.source)),
      datasets: [{
        data: summary.sourceBreakdown.map((s) => s.count),
        backgroundColor: summary.sourceBreakdown.map((_, i) => SOURCE_COLORS[i % SOURCE_COLORS.length]),
      }],
    };
  }

  // Jobs with zero applications in the current filter are dropped from the
  // chart (a sea of empty bars adds noise, not signal) but stay in the table
  // below, which lists every posting regardless of activity.
  private buildJobChart(summary: RecruitmentKpiSummary): void {
    const rows = summary.jobKpis
      .filter((j) => j.applications > 0)
      .sort((a, b) => b.applications - a.applications)
      .slice(0, CHART_ROW_LIMIT);
    this.jobChartData = {
      labels: rows.map((j) => j.jobTitle),
      datasets: [
        { label: 'Applications', data: rows.map((j) => j.applications), backgroundColor: APPLICATIONS_HUE, borderRadius: 4, maxBarThickness: 32 },
        { label: 'Hired', data: rows.map((j) => j.hired), backgroundColor: HIRED_HUE, borderRadius: 4, maxBarThickness: 32 },
      ],
    };
  }

  private buildRecruiterChart(summary: RecruitmentKpiSummary): void {
    const rows = summary.recruiterKpis
      .filter((r) => r.applications > 0)
      .slice(0, CHART_ROW_LIMIT);
    this.recruiterChartData = {
      labels: rows.map((r) => r.recruiterName || 'Unassigned'),
      datasets: [
        { label: 'Applications', data: rows.map((r) => r.applications), backgroundColor: APPLICATIONS_HUE, borderRadius: 4, maxBarThickness: 32 },
        { label: 'Hires', data: rows.map((r) => r.hires), backgroundColor: HIRED_HUE, borderRadius: 4, maxBarThickness: 32 },
      ],
    };
  }

  sourceLabel(source: string): string {
    return source.replace(/_/g, ' ');
  }

  daysLabel(value: number | undefined): string {
    return value == null ? '—' : `${value} days`;
  }

  pctLabel(value: number | undefined): string {
    return value == null ? '—' : `${value}%`;
  }
}
