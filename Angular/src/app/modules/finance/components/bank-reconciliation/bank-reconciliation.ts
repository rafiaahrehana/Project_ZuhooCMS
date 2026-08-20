import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BankReconciliation, BankReconciliationRequest, ChartOfAccount, ReconciliationTransaction, StatementImportResult } from '../../models/finance.model';
import { BankReconciliationService } from '../../services/bank-reconciliation.service';
import { CoaService } from '../../services/coa.service';
import { ApiService } from '../../../../core/services/api.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
// Anything smaller than a cent is rounding noise, not a real discrepancy - matches
// the backend's TOLERANCE in BankReconciliationServiceImpl.
const TOLERANCE = 0.01;

@Component({
  selector: 'app-bank-reconciliation',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './bank-reconciliation.html',
})
export class BankReconciliationPage implements OnInit {
  items: BankReconciliation[] = [];
  pending: BankReconciliation[] = [];
  accounts: ChartOfAccount[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  showForm = false;
  form: BankReconciliationRequest = this.emptyForm();

  // Workspace: the reconciliation currently open for matching transactions.
  selected: BankReconciliation | null = null;
  transactions: ReconciliationTransaction[] = [];
  loadingTransactions = false;
  togglingId: number | null = null;
  reconcileNotes = '';
  reconciling = false;
  uploadingStatement = false;
  importing = false;
  importResult: StatementImportResult | null = null;

  constructor(
    private reconService: BankReconciliationService,
    private coaService: CoaService,
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    // Only real bank/cash accounts make sense to reconcile against a bank statement -
    // without this filter, any ASSET-type account (Fixed Assets, Accounts Receivable,
    // an equipment account, etc.) looked identical in this picker.
    this.coaService.list(0, 200).subscribe({
      next: (res) => { this.accounts = res.content.filter((a) => a.isBankAccount); this.cdr.markForCheck(); },
    });
    this.load();
    this.loadPending();
  }

  emptyForm(): BankReconciliationRequest {
    return { bankAccountId: 0, bankStatementBalance: 0 };
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.reconService.list(this.page).subscribe({
      next: (res) => {
        this.items = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load reconciliations';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadPending(): void {
    this.reconService.getPending().subscribe({ next: (res) => { this.pending = res; this.cdr.markForCheck(); } });
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.showForm = true;
  }

  save(): void {
    if (!this.form.bankAccountId || !this.form.bankStatementBalance) return;
    this.reconService.create(this.form).subscribe({
      next: (created) => {
        this.showForm = false;
        this.success = 'Reconciliation created';
        this.cdr.markForCheck();
        this.load();
        this.loadPending();
        this.view(created);
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to create reconciliation'; this.cdr.markForCheck(); },
    });
  }

  view(item: BankReconciliation): void {
    this.selected = item;
    this.reconcileNotes = item.discrepancyNotes || '';
    this.loadingTransactions = true;
    this.cdr.markForCheck();
    this.reconService.unclearedTransactions(item.id).subscribe({
      next: (res) => {
        this.transactions = res;
        this.loadingTransactions = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load transactions';
        this.loadingTransactions = false;
        this.cdr.markForCheck();
      },
    });
  }

  close(): void {
    this.selected = null;
    this.transactions = [];
  }

  // Checking a transaction off marks it cleared against the bank statement, which
  // removes it from the outstanding total and recomputes the difference server-side.
  toggle(txn: ReconciliationTransaction): void {
    if (!this.selected || this.selected.reconciled || this.togglingId) return;
    this.togglingId = txn.id;
    const cleared = !txn.isReconciled;
    this.cdr.markForCheck();
    this.reconService.toggleTransaction(this.selected.id, txn.id, cleared).subscribe({
      next: (updated) => {
        txn.isReconciled = cleared;
        this.selected = updated;
        this.togglingId = null;
        this.cdr.markForCheck();
        this.updateInLists(updated);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update transaction';
        this.togglingId = null;
        this.cdr.markForCheck();
      },
    });
  }

  private updateInLists(updated: BankReconciliation): void {
    const i = this.items.findIndex((r) => r.id === updated.id);
    if (i > -1) this.items[i] = updated;
    const p = this.pending.findIndex((r) => r.id === updated.id);
    if (p > -1) this.pending[p] = updated;
  }

  isBalanced(item: BankReconciliation | null): boolean {
    return !!item && Math.abs(item.difference) < TOLERANCE;
  }

  confirmReconcile(): void {
    if (!this.selected || !this.isBalanced(this.selected)) return;
    this.reconciling = true;
    this.cdr.markForCheck();
    this.reconService.markAsReconciled(this.selected.id, this.reconcileNotes || undefined).subscribe({
      next: () => {
        this.reconciling = false;
        this.success = 'Marked as reconciled';
        this.close();
        this.cdr.markForCheck();
        this.load();
        this.loadPending();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to reconcile';
        this.reconciling = false;
        this.cdr.markForCheck();
      },
    });
  }

  onStatementCsvSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.selected) return;

    this.importing = true;
    this.importResult = null;
    this.error = '';
    this.cdr.markForCheck();

    this.reconService.importStatement(this.selected.id, file).subscribe({
      next: (result) => {
        this.importing = false;
        this.importResult = result;
        this.selected = result.reconciliation;
        this.updateInLists(result.reconciliation);
        this.cdr.markForCheck();
        // Reload the checklist - matched transactions are now cleared.
        this.view(result.reconciliation);
      },
      error: (err) => {
        this.importing = false;
        this.error = err?.error?.message || 'Failed to import statement';
        this.cdr.markForCheck();
      },
    });
    input.value = '';
  }

  onStatementSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.selected) return;

    this.uploadingStatement = true;
    this.error = '';
    this.cdr.markForCheck();

    this.api.uploadFile(file).subscribe({
      next: (uploaded) => {
        this.reconService.attachStatement(this.selected!.id, uploaded.fileName, uploaded.fileUrl).subscribe({
          next: (updated) => {
            this.selected = updated;
            this.uploadingStatement = false;
            this.cdr.markForCheck();
            this.updateInLists(updated);
          },
          error: (err) => {
            this.error = err?.error?.message || 'Failed to attach statement';
            this.uploadingStatement = false;
            this.cdr.markForCheck();
          },
        });
      },
      error: () => {
        this.error = 'Failed to upload file';
        this.uploadingStatement = false;
        this.cdr.markForCheck();
      },
    });
    input.value = '';
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
