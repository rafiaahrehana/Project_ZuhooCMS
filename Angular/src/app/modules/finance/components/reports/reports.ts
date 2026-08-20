import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { ProfitLossReport, BalanceSheetReport, TrialBalanceReport, AgeingReport, CashFlowReport, ApAgeingReport, AccountLedgerReport, ChartOfAccount } from '../../models/finance.model';
import { FinancialReportService } from '../../services/financial-report.service';
import { VendorBillService } from '../../services/vendor.service';
import { CoaService } from '../../services/coa.service';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
type ReportType = 'PROFIT_LOSS' | 'BALANCE_SHEET' | 'TRIAL_BALANCE' | 'AGEING' | 'AP_AGEING' | 'CASH_FLOW' | 'ACCOUNT_LEDGER';

// Validated categorical slots (see dataviz skill palette) - fixed order, never
// reassigned per value. Aqua falls below 3:1 contrast on a light surface, so
// both series carry direct value labels (the "relief rule") rather than relying
// on color alone.
const SERIES_1_BLUE = '#2a78d6';
const SERIES_2_AQUA = '#1baf7a';
const STATUS_GOOD = '#0ca30c';
const STATUS_CRITICAL = '#d03b3b';

interface ReportCard {
  type: ReportType;
  label: string;
  icon: string;
  desc: string;
}

/** Card picker groups, ERP-style - clicking a card opens that report's own filter form. */
interface ReportGroup {
  title: string;
  icon: string;
  /** Matches app-stat-card's variant-* palette, so the group tile reads like the rest of the app's KPI cards. */
  variant: 'primary' | 'success' | 'info' | 'purple';
  cards: ReportCard[];
}

interface ExternalReport {
  group: string;
  label: string;
  icon: string;
  desc: string;
  link: string;
}

@Component({
  selector: 'app-finance-reports',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, RouterLink, BaseChartDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reports.html',
  styleUrl: './reports.scss',
})
export class Reports {
  readonly reportGroups: ReportGroup[] = [
    { title: 'Financial Statements', icon: 'bi-graph-up-arrow', variant: 'primary', cards: [
      { type: 'PROFIT_LOSS', label: 'Profit & Loss', icon: 'bi-graph-up-arrow', desc: 'Revenue vs expense over a period' },
      { type: 'BALANCE_SHEET', label: 'Balance Sheet', icon: 'bi-bar-chart-steps', desc: 'Assets, liabilities and equity as of a date' },
      { type: 'TRIAL_BALANCE', label: 'Trial Balance', icon: 'bi-list-check', desc: 'Every account\'s debit/credit balance' },
    ]},
    { title: 'Receivables & Payables', icon: 'bi-cash-stack', variant: 'success', cards: [
      { type: 'AGEING', label: 'AR Ageing', icon: 'bi-cash-coin', desc: 'What clients owe, by how overdue' },
      { type: 'AP_AGEING', label: 'AP Ageing', icon: 'bi-credit-card', desc: 'What we owe vendors, by how overdue' },
    ]},
    { title: 'Cash & Banking', icon: 'bi-bank', variant: 'info', cards: [
      { type: 'CASH_FLOW', label: 'Cash Flow', icon: 'bi-bank', desc: 'Cash in vs out by category' },
    ]},
    { title: 'Accounting', icon: 'bi-journal-text', variant: 'purple', cards: [
      { type: 'ACCOUNT_LEDGER', label: 'Account Ledger', icon: 'bi-journal-text', desc: 'Every transaction on one account' },
    ]},
  ];

  /** External pages that belong in the picker but aren't generated here. */
  readonly externalReports: ExternalReport[] = [
    { group: 'Cash & Banking', label: 'Bank Reconciliation', icon: 'bi-bank2', desc: 'Match the bank statement to the ledger', link: '/finance/bank-reconciliation' },
    { group: 'Accounting', label: 'General Ledger', icon: 'bi-journal-bookmark', desc: 'The full posted-transaction feed', link: '/finance/general-ledger' },
    { group: 'Accounting', label: 'Journal Report', icon: 'bi-journal-plus', desc: 'Manual and system journal entries', link: '/finance/journal-entries' },
  ];

  /** null = the card grid; set = that report's filter form + results. */
  selected: ReportType | null = null;

  catalogSearch = '';

  /** 'ALL', 'FAVORITES', or a group title - drives the quick-filter pill row. */
  activeCategory = 'ALL';

  private static readonly FAVORITES_KEY = 'bos-favorite-reports';

  /** External reports have no ReportType, so favorites are keyed by a string id instead. */
  favoriteIds = new Set<string>(this.loadFavorites());

  cardId(c: ReportCard): string { return c.type; }
  externalId(e: ExternalReport): string { return 'ext:' + e.label; }

  isFavorite(id: string): boolean { return this.favoriteIds.has(id); }

  toggleFavorite(id: string, event: Event): void {
    event.stopPropagation();
    if (this.favoriteIds.has(id)) this.favoriteIds.delete(id);
    else this.favoriteIds.add(id);
    // New Set so the getter below (read via the template) sees the change under OnPush.
    this.favoriteIds = new Set(this.favoriteIds);
    this.saveFavorites();
  }

  private loadFavorites(): string[] {
    try {
      const raw = localStorage.getItem(Reports.FAVORITES_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch { return []; }
  }

  private saveFavorites(): void {
    try { localStorage.setItem(Reports.FAVORITES_KEY, JSON.stringify([...this.favoriteIds])); } catch { /* private mode */ }
  }

  /** Quick-filter pills shown under the search box: All, Favorites, then one per group. */
  get categoryPills(): string[] {
    return ['ALL', 'FAVORITES', ...this.reportGroups.map((g) => g.title)];
  }

  categoryLabel(c: string): string {
    return c === 'ALL' ? 'All Reports' : c === 'FAVORITES' ? 'Favorites' : c;
  }

  /** Grouped cards filtered by the search box and the active category pill; groups with no matches dropped entirely. */
  get filteredGroups(): { title: string; icon: string; variant: string; cards: ReportCard[]; external: ExternalReport[] }[] {
    const term = this.catalogSearch.trim().toLowerCase();
    return this.reportGroups
      .filter((g) => this.activeCategory === 'ALL' || this.activeCategory === 'FAVORITES' || this.activeCategory === g.title)
      .map((g) => ({
        title: g.title,
        icon: g.icon,
        variant: g.variant,
        cards: g.cards.filter((c) =>
          (!term || c.label.toLowerCase().includes(term) || c.desc.toLowerCase().includes(term))
          && (this.activeCategory !== 'FAVORITES' || this.favoriteIds.has(this.cardId(c)))),
        external: this.externalReports.filter((e) => e.group === g.title
          && (!term || e.label.toLowerCase().includes(term) || e.desc.toLowerCase().includes(term))
          && (this.activeCategory !== 'FAVORITES' || this.favoriteIds.has(this.externalId(e)))),
      }))
      .filter((g) => g.cards.length > 0 || g.external.length > 0);
  }

  openReport(type: ReportType): void {
    this.selected = type;
    this.reportType = type;
    this.onReportTypeChange();
  }

  backToReports(): void {
    this.selected = null;
    this.profitLoss = undefined;
    this.balanceSheet = undefined;
    this.trialBalance = undefined;
    this.ageing = undefined;
    this.apAgeing = undefined;
    this.cashFlow = undefined;
    this.accountLedger = undefined;
    this.error = '';
  }

  reportLabel(type: ReportType): string {
    return this.reportGroups.flatMap((g) => g.cards).find((c) => c.type === type)?.label || '';
  }

  reportType: ReportType = 'PROFIT_LOSS';
  startDate = '';
  endDate = '';
  asOfDate = '';

  profitLoss?: ProfitLossReport;
  balanceSheet?: BalanceSheetReport;
  trialBalance?: TrialBalanceReport;
  ageing?: AgeingReport;
  apAgeing?: ApAgeingReport;
  cashFlow?: CashFlowReport;
  accountLedger?: AccountLedgerReport;

  // For the Account Ledger account picker (lazy-loaded on first use)
  accounts: ChartOfAccount[] = [];
  ledgerAccountId: number | null = null;

  loading = false;
  error = '';

  plChartData?: ChartConfiguration<'bar'>['data'];
  plChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } },
  };

  tbChartData?: ChartConfiguration<'bar'>['data'];
  tbChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
    scales: { y: { beginAtZero: true } },
  };

  ageingChartData?: ChartConfiguration<'bar'>['data'];
  ageingChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: { y: { beginAtZero: true } },
  };

  cashFlowChartData?: ChartConfiguration<'bar'>['data'];
  cashFlowChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'top' } },
    scales: { y: { beginAtZero: true } },
  };

  constructor(
    private reportService: FinancialReportService,
    private vendorBillService: VendorBillService,
    private coaService: CoaService,
    private cdr: ChangeDetectorRef,
  ) {}

  usesDateRange(): boolean {
    return this.reportType === 'PROFIT_LOSS' || this.reportType === 'CASH_FLOW' || this.reportType === 'ACCOUNT_LEDGER';
  }

  onReportTypeChange(): void {
    if (this.reportType === 'ACCOUNT_LEDGER' && !this.accounts.length) {
      this.coaService.list(0, 200).subscribe({
        next: (res) => { this.accounts = res.content; this.cdr.markForCheck(); },
        error: () => { this.error = 'Failed to load accounts'; this.cdr.markForCheck(); },
      });
    }
  }

  generate(): void {
    this.error = '';
    this.profitLoss = undefined;
    this.balanceSheet = undefined;
    this.trialBalance = undefined;
    this.ageing = undefined;
    this.apAgeing = undefined;
    this.cashFlow = undefined;
    this.accountLedger = undefined;
    this.plChartData = undefined;
    this.tbChartData = undefined;
    this.ageingChartData = undefined;
    this.cashFlowChartData = undefined;

    if (this.usesDateRange()) {
      if (!this.startDate || !this.endDate) {
        this.error = 'Please select a start and end date';
        this.cdr.markForCheck();
        return;
      }
      if (this.reportType === 'ACCOUNT_LEDGER') {
        if (!this.ledgerAccountId) {
          this.error = 'Please select an account';
          this.cdr.markForCheck();
          return;
        }
        this.loading = true;
        this.cdr.markForCheck();
        this.reportService.accountLedger(this.ledgerAccountId, this.startDate, this.endDate).subscribe({
          next: (r) => { this.accountLedger = r; this.loading = false; this.cdr.markForCheck(); },
          error: () => { this.error = 'Failed to generate report'; this.loading = false; this.cdr.markForCheck(); },
        });
        return;
      }
      this.loading = true;
      this.cdr.markForCheck();
      if (this.reportType === 'PROFIT_LOSS') {
        this.reportService.profitLoss(this.startDate, this.endDate).subscribe({
          next: (r) => { this.profitLoss = r; this.buildProfitLossChart(r); this.loading = false; this.cdr.markForCheck(); },
          error: () => { this.error = 'Failed to generate report'; this.loading = false; this.cdr.markForCheck(); },
        });
      } else {
        this.reportService.cashFlow(this.startDate, this.endDate).subscribe({
          next: (r) => { this.cashFlow = r; this.buildCashFlowChart(r); this.loading = false; this.cdr.markForCheck(); },
          error: () => { this.error = 'Failed to generate report'; this.loading = false; this.cdr.markForCheck(); },
        });
      }
      return;
    }

    if (!this.asOfDate) {
      this.error = 'Please select an as-of date';
      this.cdr.markForCheck();
      return;
    }
    this.loading = true;
    this.cdr.markForCheck();
    if (this.reportType === 'BALANCE_SHEET') {
      this.reportService.balanceSheet(this.asOfDate).subscribe({
        next: (r) => { this.balanceSheet = r; this.loading = false; this.cdr.markForCheck(); },
        error: () => { this.error = 'Failed to generate report'; this.loading = false; this.cdr.markForCheck(); },
      });
    } else if (this.reportType === 'TRIAL_BALANCE') {
      this.reportService.trialBalance(this.asOfDate).subscribe({
        next: (r) => { this.trialBalance = r; this.buildTrialBalanceChart(r); this.loading = false; this.cdr.markForCheck(); },
        error: () => { this.error = 'Failed to generate report'; this.loading = false; this.cdr.markForCheck(); },
      });
    } else if (this.reportType === 'AP_AGEING') {
      this.vendorBillService.apAgeing(this.asOfDate).subscribe({
        next: (r) => { this.apAgeing = r; this.buildApAgeingChart(r); this.loading = false; this.cdr.markForCheck(); },
        error: () => { this.error = 'Failed to generate report'; this.loading = false; this.cdr.markForCheck(); },
      });
    } else {
      this.reportService.ageing(this.asOfDate).subscribe({
        next: (r) => { this.ageing = r; this.buildAgeingChart(r); this.loading = false; this.cdr.markForCheck(); },
        error: () => { this.error = 'Failed to generate report'; this.loading = false; this.cdr.markForCheck(); },
      });
    }
  }

  get netProfitClass(): string {
    if (!this.profitLoss) return '';
    return this.profitLoss.netProfit >= 0 ? 'text-success' : 'text-danger';
  }

  netProfitColor(): string {
    if (!this.profitLoss) return STATUS_GOOD;
    return this.profitLoss.netProfit >= 0 ? STATUS_GOOD : STATUS_CRITICAL;
  }

  get netChangeClass(): string {
    if (!this.cashFlow) return '';
    return this.cashFlow.netChange >= 0 ? 'text-success' : 'text-danger';
  }

  private buildProfitLossChart(r: ProfitLossReport): void {
    this.plChartData = {
      labels: ['Revenue', 'Expense'],
      datasets: [{
        data: [r.totalRevenue, r.totalExpense],
        backgroundColor: [SERIES_1_BLUE, SERIES_2_AQUA],
        borderRadius: 4,
        maxBarThickness: 80,
      }],
    };
  }

  private buildTrialBalanceChart(r: TrialBalanceReport): void {
    this.tbChartData = {
      labels: r.accounts.map((a) => a.accountCode),
      datasets: [
        { label: 'Debit', data: r.accounts.map((a) => a.debitBalance), backgroundColor: SERIES_1_BLUE, borderRadius: 3 },
        { label: 'Credit', data: r.accounts.map((a) => a.creditBalance), backgroundColor: SERIES_2_AQUA, borderRadius: 3 },
      ],
    };
  }

  // Aging buckets are ordered by severity (not distinct identities), so a single
  // hue is correct here rather than the categorical palette.
  private buildAgeingChart(r: AgeingReport): void {
    this.ageingChartData = {
      labels: ['Current', '1-30 days', '31-60 days', '61-90 days', '90+ days'],
      datasets: [{
        data: [r.current, r.days1to30, r.days31to60, r.days61to90, r.over90],
        backgroundColor: SERIES_1_BLUE,
        borderRadius: 4,
        maxBarThickness: 60,
      }],
    };
  }

  private buildApAgeingChart(r: ApAgeingReport): void {
    this.ageingChartData = {
      labels: ['Current', '1-30 days', '31-60 days', '61-90 days', '90+ days'],
      datasets: [{
        data: [r.current, r.days1to30, r.days31to60, r.days61to90, r.over90],
        backgroundColor: SERIES_2_AQUA,
        borderRadius: 4,
        maxBarThickness: 60,
      }],
    };
  }

  private buildCashFlowChart(r: CashFlowReport): void {
    this.cashFlowChartData = {
      labels: r.lines.map((l) => l.category),
      datasets: [
        { label: 'Inflow', data: r.lines.map((l) => l.inflow), backgroundColor: SERIES_1_BLUE, borderRadius: 3 },
        { label: 'Outflow', data: r.lines.map((l) => l.outflow), backgroundColor: SERIES_2_AQUA, borderRadius: 3 },
      ],
    };
  }
}
