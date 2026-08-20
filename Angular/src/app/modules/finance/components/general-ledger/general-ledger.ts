import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ChartOfAccount, GeneralLedgerEntry } from '../../models/finance.model';
import { GeneralLedgerService } from '../../services/general-ledger.service';
import { CoaService } from '../../services/coa.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-general-ledger',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './general-ledger.html',
  styleUrl: './general-ledger.scss',
})
export class GeneralLedger implements OnInit {
  entries: GeneralLedgerEntry[] = [];
  accounts: ChartOfAccount[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  accountFilter?: number;
  startDate = '';
  endDate = '';
  accountBalance: number | null = null;

  reconcileTarget: GeneralLedgerEntry | null = null;
  reconcileNotes = '';

  constructor(private ledgerService: GeneralLedgerService, private coaService: CoaService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.coaService.list(0, 200).subscribe({ next: (res) => { this.accounts = res.content; this.cdr.markForCheck(); } });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.accountBalance = null;
    this.cdr.markForCheck();
    let obs;
    if (this.startDate && this.endDate) {
      obs = this.ledgerService.getByDateRange(this.startDate, this.endDate, this.page);
    } else if (this.accountFilter) {
      obs = this.ledgerService.getByAccount(this.accountFilter, this.page);
      this.ledgerService.getAccountBalance(this.accountFilter).subscribe({ next: (b) => { this.accountBalance = b; this.cdr.markForCheck(); } });
    } else {
      obs = this.ledgerService.list(this.page);
    }
    obs.subscribe({
      next: (res) => {
        this.entries = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load ledger entries';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.load();
  }

  clearFilters(): void {
    this.accountFilter = undefined;
    this.startDate = '';
    this.endDate = '';
    this.page = 0;
    this.load();
  }

  openReconcile(e: GeneralLedgerEntry): void {
    this.reconcileTarget = e;
    this.reconcileNotes = '';
  }

  doReconcile(): void {
    if (!this.reconcileTarget) return;
    this.ledgerService.reconcile(this.reconcileTarget.id, this.reconcileNotes || undefined).subscribe({
      next: () => {
        this.reconcileTarget = null;
        this.success = 'Entry reconciled';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to reconcile';
        this.reconcileTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  // Exports only the currently-loaded page of entries (client-side, no backend
  // export endpoint exists for General Ledger).
  exportCsv(): void {
    const headers = ['Date', 'Account', 'Type', 'Description', 'Debit', 'Credit', 'Reconciled'];
    const rows = this.entries.map((e) => [
      e.transactionDate,
      e.accountName,
      e.accountType || '',
      (e.description || '').replace(/"/g, '""'),
      e.debitAmount ?? 0,
      e.creditAmount ?? 0,
      e.isReconciled ? 'Yes' : 'No',
    ]);
    const csv = [headers, ...rows].map((r) => r.map((v) => `"${v}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'general-ledger.csv';
    a.click();
    URL.revokeObjectURL(url);
  }
}
