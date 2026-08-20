import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChartOfAccount, JournalEntry, JournalEntryLineRequest, JournalEntryRequest } from '../../models/finance.model';
import { JournalEntryService } from '../../services/journal-entry.service';
import { CoaService } from '../../services/coa.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-journal-entries',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './journal-entries.html',
  styleUrl: './journal-entries.scss',
})
export class JournalEntries implements OnInit {
  // VARIABLES
  entries: JournalEntry[] = [];
  accounts: ChartOfAccount[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  showForm = false;
  form: JournalEntryRequest = this.emptyForm();
  saving = false;
  approveTarget: JournalEntry | null = null;
  postTarget: JournalEntry | null = null;
  deleteTarget: JournalEntry | null = null;
  reverseTarget: JournalEntry | null = null;

  // Filters (client-side, over the loaded page)
  searchTerm = '';
  statusFilter = '';

  get filteredEntries(): JournalEntry[] {
    const term = this.searchTerm.trim().toLowerCase();
    return this.entries.filter((e) => {
      const matchesStatus = !this.statusFilter || this.statusLabel(e) === this.statusFilter;
      const matchesTerm =
        !term ||
        (e.journalEntryNumber || '').toLowerCase().includes(term) ||
        (e.description || '').toLowerCase().includes(term);
      return matchesStatus && matchesTerm;
    });
  }

  get draftCount(): number { return this.entries.filter((e) => !e.approved && !e.posted).length; }
  get approvedCount(): number { return this.entries.filter((e) => e.approved && !e.posted).length; }
  get postedCount(): number { return this.entries.filter((e) => e.posted).length; }
  get visibleAmount(): number { return this.filteredEntries.reduce((s, e) => s + (e.amount || 0), 0); }

  clearFilters(): void {
    this.searchTerm = '';
    this.statusFilter = '';
  }

  constructor(
    private journalEntryService: JournalEntryService,
    private coaService: CoaService,
    private location: Location,
    private cdr: ChangeDetectorRef,
  ) {}

  goBack(): void {
    this.location.back();
  }

  // LIFECYCLE HOOKS
  ngOnInit(): void {
    this.load();
  }

  // LOAD JOURNAL ENTRIES
  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.journalEntryService.list(this.page).subscribe({
      next: (res) => {
        this.entries = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load journal entries';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  // LOAD ACCOUNTS FOR DROPDOWNS
  loadAccounts(): void {
    if (this.accounts.length) return;
    this.coaService.list(0, 200).subscribe({
      next: (res) => { this.accounts = res.content; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load accounts'; this.cdr.markForCheck(); },
    });
  }

  private emptyForm(): JournalEntryRequest {
    return {
      entryDate: new Date().toISOString().slice(0, 10),
      lines: [
        { accountId: null, debitAmount: 0, creditAmount: 0 },
        { accountId: null, debitAmount: 0, creditAmount: 0 },
      ],
      description: '',
      notes: '',
    };
  }

  // OPEN CREATE FORM
  openCreate(): void {
    this.form = this.emptyForm();
    this.showForm = true;
    this.loadAccounts();
  }

  // ── Multi-line form helpers ────────────────────────────────
  addLine(): void {
    this.form.lines.push({ accountId: null, debitAmount: 0, creditAmount: 0 });
  }

  removeLine(index: number): void {
    if (this.form.lines.length <= 2) return; // a JE needs at least 2 lines
    this.form.lines.splice(index, 1);
  }

  get totalDebits(): number {
    return this.form.lines.reduce((s, l) => s + (l.debitAmount || 0), 0);
  }

  get totalCredits(): number {
    return this.form.lines.reduce((s, l) => s + (l.creditAmount || 0), 0);
  }

  get isBalanced(): boolean {
    return Math.abs(this.totalDebits - this.totalCredits) < 0.01 && this.totalDebits > 0;
  }

  get canSave(): boolean {
    return (
      this.isBalanced &&
      !!this.form.entryDate &&
      this.form.lines.every(
        (l) => l.accountId != null && ((l.debitAmount || 0) > 0) !== ((l.creditAmount || 0) > 0),
      )
    );
  }

  // A line is debit OR credit - typing into one side clears the other so users
  // can't accidentally submit a line with both.
  onDebitChanged(line: JournalEntryLineRequest): void {
    if ((line.debitAmount || 0) > 0) line.creditAmount = 0;
  }

  onCreditChanged(line: JournalEntryLineRequest): void {
    if ((line.creditAmount || 0) > 0) line.debitAmount = 0;
  }

  // SAVE JOURNAL ENTRY
  save(): void {
    if (!this.canSave || this.saving) return;
    this.saving = true;
    this.cdr.markForCheck();
    this.journalEntryService.create(this.form).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.form = this.emptyForm();
        this.success = 'Journal entry created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to create journal entry';
        this.cdr.markForCheck();
      },
    });
  }

  // APPROVE ENTRY
  doApprove(): void {
    if (!this.approveTarget) return;
    this.journalEntryService.approve(this.approveTarget.id).subscribe({
      next: () => {
        this.approveTarget = null;
        this.success = 'Journal entry approved';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to approve';
        this.approveTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  // POST ENTRY TO LEDGER
  doPost(): void {
    if (!this.postTarget) return;
    this.journalEntryService.post(this.postTarget.id).subscribe({
      next: () => {
        this.postTarget = null;
        this.success = 'Journal entry posted';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to post';
        this.postTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  // DELETE ENTRY
  doDelete(): void {
    if (!this.deleteTarget) return;
    this.journalEntryService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Journal entry deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Cannot delete journal entry';
        this.cdr.markForCheck();
      },
    });
  }

  // PAGINATION
  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  // REVERSE ENTRY
  doReverse(): void {
    if (!this.reverseTarget) return;
    this.journalEntryService.reverse(this.reverseTarget.id).subscribe({
      next: () => {
        this.reverseTarget = null;
        this.success = 'Journal entry reversed';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to reverse';
        this.reverseTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  // STATUS HELPERS
  statusLabel(e: JournalEntry): string {
    return e.reversed ? 'REVERSED' : e.posted ? 'POSTED' : e.approved ? 'APPROVED' : 'DRAFT';
  }
  statusClass(e: JournalEntry): string {
    return e.reversed ? 'text-bg-secondary' : e.posted ? 'text-bg-primary' : e.approved ? 'text-bg-success' : 'text-bg-secondary';
  }
  statusSoftClass(e: JournalEntry): string {
    return e.reversed ? 'badge-soft-secondary' : e.posted ? 'badge-soft-success' : e.approved ? 'badge-soft-info' : 'badge-soft-secondary';
  }
}
