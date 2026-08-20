import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HrDashboardService, HrDashboardSummary, DepartmentSlice } from '../../services/hr-dashboard.service';
import { AnnouncementService } from '../../services/announcement.service';
import { Announcement } from '../../models/hrm.model';
import { Loader } from '../../../../shared/components/loader/loader';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

interface QuickAction { label: string; icon: string; link: string; accent: string; }

/** Donut segment with its stroke geometry precomputed. */
interface DonutSegment extends DepartmentSlice { colour: string; dash: string; offset: number; }

const DONUT_COLOURS = ['#8b5cf6', '#2563eb', '#0d9488', '#f59e0b', '#e11d48', '#6366f1', '#94a3b8'];

@Component({
  selector: 'app-hr-dashboard',
  imports: [CommonModule, RouterLink, Loader, HasPermissionDirective],
  templateUrl: './hr-dashboard.html',
  styleUrl: './hr-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HrDashboard implements OnInit {
  summary?: HrDashboardSummary;
  announcements: Announcement[] = [];
  loading = false;
  error = '';

  readonly today = new Date();

  readonly quickActions: QuickAction[] = [
    { label: 'Add Employee',     icon: 'bi-person-plus',    link: '/hrm/employees',    accent: '#8b5cf6' },
    { label: 'Departments',      icon: 'bi-diagram-3',      link: '/hrm/departments',  accent: '#2563eb' },
    { label: 'Leave Requests',   icon: 'bi-calendar-plus',  link: '/hrm/leaves',       accent: '#0d9488' },
    { label: 'Attendance',       icon: 'bi-calendar-check', link: '/attendance/records', accent: '#6366f1' },
    { label: 'Payroll',          icon: 'bi-cash-stack',     link: '/hrm/payroll',      accent: '#f59e0b' },
    { label: 'Recruitment',      icon: 'bi-briefcase',      link: '/hrm/job-postings', accent: '#e11d48' },
    { label: 'Performance',      icon: 'bi-graph-up-arrow', link: '/hrm/performance',  accent: '#7c3aed' },
    { label: 'Announcements',    icon: 'bi-megaphone',      link: '/hrm/announcements', accent: '#0891b2' },
  ];

  constructor(
    private hrDashboard: HrDashboardService,
    private announcementService: AnnouncementService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.announcementService.listActive().subscribe({
      next: (list) => { this.announcements = (list || []).slice(0, 3); this.cdr.markForCheck(); },
      error: () => { this.announcements = []; this.cdr.markForCheck(); },
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.hrDashboard.summary().subscribe({
      next: (s) => { this.summary = s; this.loading = false; this.cdr.markForCheck(); },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load the HR dashboard';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  // ── Donut ───────────────────────────────────────────────────
  // Drawn as an SVG circle with stroke-dasharray, so there is no chart library
  // in the bundle for one visual.

  readonly donutRadius = 54;
  get donutCircumference(): number { return 2 * Math.PI * this.donutRadius; }

  get donutSegments(): DonutSegment[] {
    const slices = this.summary?.departmentDistribution ?? [];
    const c = this.donutCircumference;
    let consumed = 0;
    return slices.map((s, i) => {
      const len = (s.percent / 100) * c;
      const seg: DonutSegment = {
        ...s,
        colour: DONUT_COLOURS[i % DONUT_COLOURS.length],
        dash: `${len} ${c - len}`,
        // Negative offset walks each segment clockwise from the top.
        offset: -consumed,
      };
      consumed += len;
      return seg;
    });
  }

  // ── Headcount sparkline ─────────────────────────────────────

  /** Points for a month-to-date headcount polyline in a 100x32 viewBox. */
  get trendPath(): string {
    const pts = this.summary?.headcountTrend ?? [];
    if (pts.length < 2) return '';
    const counts = pts.map(p => p.headcount);
    const min = Math.min(...counts);
    const max = Math.max(...counts);
    const span = max - min || 1;
    return pts
      .map((p, i) => {
        const x = (i / (pts.length - 1)) * 100;
        const y = 32 - ((p.headcount - min) / span) * 28 - 2;
        return `${x.toFixed(1)},${y.toFixed(1)}`;
      })
      .join(' ');
  }

  get trendHasData(): boolean {
    return (this.summary?.headcountTrend?.length ?? 0) >= 2;
  }

  /** Total applications across the funnel, for bar scaling. */
  get pipelineTotal(): number {
    return (this.summary?.recruitmentPipeline ?? []).reduce((a, s) => a + s.count, 0);
  }

  /** Share of the month's leave requests in a given state, for the bars. */
  leavePercent(part: number): number {
    const total = this.summary?.leaveSummary?.total ?? 0;
    if (total <= 0) return 0;
    return Math.round((part / total) * 100);
  }

  upcomingIcon(kind: string): string {
    return kind === 'BIRTHDAY' ? 'bi-gift' : 'bi-hourglass-split';
  }
}
