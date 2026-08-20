import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChartOfAccount } from '../../models/finance.model';
import { CoaService } from '../../services/coa.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-chart-of-accounts',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './chart-of-accounts.html',
})
export class ChartOfAccounts implements OnInit {
  accounts: ChartOfAccount[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  showForm = false;
  editId: number | null = null;
  deleteTarget: ChartOfAccount | null = null;
  // openingBalance/-Date only apply on create: the backend posts a balanced entry
  // against Opening Balance Equity so the ledger backs the starting balance up.
  form: Partial<ChartOfAccount> & { openingBalance?: number; openingBalanceDate?: string } = {};
  accountTypes = ['ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE', 'CONTRA_ASSET', 'CONTRA_LIABILITY', 'CONTRA_REVENUE'];

  constructor(private coaService: CoaService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.coaService.list(this.page).subscribe({
      next: (res) => {
        this.accounts = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load accounts';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreate(): void {
    this.editId = null;
    this.form = { active: true, allowDirectPosting: true };
    this.showForm = true;
    this.error = '';
  }

  openEdit(a: ChartOfAccount): void {
    this.editId = a.id;
    this.form = {
      accountCode: a.accountCode,
      accountName: a.accountName,
      type: a.type,
      description: a.description,
      notes: a.notes,
      active: a.active,
      isHeaderAccount: a.isHeaderAccount,
      isBankAccount: a.isBankAccount,
      allowDirectPosting: a.allowDirectPosting,
    };
    this.showForm = true;
    this.error = '';
  }

  save(): void {
    if (!this.form.accountCode?.trim() || !this.form.accountName?.trim() || !this.form.type) {
      this.error = 'Code, Name, and Type are required.';
      this.cdr.markForCheck();
      return;
    }
    const op = this.editId
      ? this.coaService.update(this.editId, this.form)
      : this.coaService.create(this.form);
    op.subscribe({
      next: () => {
        this.showForm = false;
        this.success = 'Saved';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save'; this.cdr.markForCheck(); },
    });
  }

  confirmDelete(a: ChartOfAccount): void {
    this.deleteTarget = a;
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.coaService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.deleteTarget = null;
        this.error = err?.error?.message || 'Cannot delete';
        this.cdr.markForCheck();
      },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  // ── Summary bar (computed from the loaded page) ───────────
  get activeCount(): number {
    return this.accounts.filter((a) => a.active).length;
  }

  get netBalance(): number {
    return this.accounts.reduce((sum, a) => sum + (a.balance || 0), 0);
  }

  get lastEntryLabel(): string {
    if (!this.accounts.length) return '—';
    const latest = this.accounts
      .map((a) => (a.createdAt ? new Date(a.createdAt).getTime() : 0))
      .reduce((max, t) => Math.max(max, t), 0);
    if (!latest) return '—';
    const mins = Math.floor((Date.now() - latest) / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins} min ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs} hour${hrs === 1 ? '' : 's'} ago`;
    const days = Math.floor(hrs / 24);
    return `${days} day${days === 1 ? '' : 's'} ago`;
  }

  // Colored badge per account type, matching the reference design.
  typeBadgeClass(type?: string): string {
    switch (type) {
      case 'ASSET': return 'badge-soft-info';
      case 'LIABILITY':
      case 'CONTRA_LIABILITY': return 'badge-soft-danger';
      case 'REVENUE':
      case 'CONTRA_REVENUE': return 'badge-soft-warning';
      case 'EXPENSE': return 'badge-soft-primary';
      case 'EQUITY':
      case 'CONTRA_ASSET': return 'badge-soft-secondary';
      default: return 'badge-soft-secondary';
    }
  }

  // Accent color per account type — drives the card's top border + soft tint.
  typeColor(type?: string): string {
    switch (type) {
      case 'ASSET': return '#2563EB';
      case 'LIABILITY':
      case 'CONTRA_LIABILITY': return '#DC2626';
      case 'REVENUE':
      case 'CONTRA_REVENUE': return '#D97706';
      case 'EXPENSE': return '#6B46FF';
      case 'EQUITY':
      case 'CONTRA_ASSET': return '#64748B';
      default: return '#64748B';
    }
  }

  typeLabel(type?: string): string {
    if (!type) return '';
    // ASSET -> Asset, CONTRA_ASSET -> Contra Asset
    return type
      .toLowerCase()
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
  }
}
