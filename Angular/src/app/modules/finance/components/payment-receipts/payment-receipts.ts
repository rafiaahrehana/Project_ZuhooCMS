import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PAYMENT_METHODS, PaymentReceipt, PaymentReceiptRequest, ChartOfAccount } from '../../models/finance.model';
import { PaymentReceiptService } from '../../services/payment-receipt.service';
import { CoaService } from '../../services/coa.service';
import { Client } from '../../../crm/models/crm.model';
import { ClientService } from '../../../crm/services/client.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-payment-receipts',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './payment-receipts.html',
})
export class PaymentReceipts implements OnInit {
  receipts: PaymentReceipt[] = [];
  clients: Client[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  showForm = false;
  form: PaymentReceiptRequest = this.emptyForm();
  methods = PAYMENT_METHODS;

  depositTarget: PaymentReceipt | null = null;
  depositBank = '';
  deleteTarget: PaymentReceipt | null = null;
  reverseTarget: PaymentReceipt | null = null;
  reverseReason = '';

  // Bank/cash accounts to deposit into — sourced from the Chart of Accounts (asset accounts).
  bankAccounts: ChartOfAccount[] = [];

  constructor(
    private receiptService: PaymentReceiptService,
    private clientService: ClientService,
    private coaService: CoaService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadBankAccounts();
  }

  loadBankAccounts(): void {
    this.coaService.list(0, 200).subscribe({
      next: (res) => {
        this.bankAccounts = (res.content || []).filter((a) => a.active && a.type === 'ASSET');
        this.cdr.markForCheck();
      },
      error: () => { this.bankAccounts = []; this.cdr.markForCheck(); },
    });
  }

  emptyForm(): PaymentReceiptRequest {
    return { clientId: 0, amount: 0, paymentDate: new Date().toISOString().slice(0, 10), paymentMethod: 'BANK_TRANSFER', transactionReference: '', notes: '' };
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.receiptService.list(this.page).subscribe({
      next: (res) => {
        this.receipts = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load payment receipts';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadClients(): void {
    if (this.clients.length) return;
    this.clientService.listActive().subscribe({ next: (res) => { this.clients = res; this.cdr.markForCheck(); }, error: () => { this.clients = []; this.cdr.markForCheck(); } });
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.showForm = true;
    this.loadClients();
  }

  save(): void {
    if (!this.form.clientId || !this.form.amount) return;
    this.receiptService.create(this.form).subscribe({
      next: () => {
        this.showForm = false;
        this.success = 'Payment receipt created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to create payment receipt'; this.cdr.markForCheck(); },
    });
  }

  confirm(r: PaymentReceipt): void {
    this.receiptService.confirm(r.id).subscribe({
      next: () => {
        this.success = 'Payment confirmed';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to confirm'; this.cdr.markForCheck(); },
    });
  }

  openDeposit(r: PaymentReceipt): void {
    this.depositTarget = r;
    this.depositBank = '';
  }

  doDeposit(): void {
    if (!this.depositTarget || !this.depositBank.trim()) return;
    this.receiptService.markAsDeposited(this.depositTarget.id, this.depositBank.trim()).subscribe({
      next: () => {
        this.depositTarget = null;
        this.success = 'Marked as deposited';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to mark as deposited';
        this.depositTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  doReverse(): void {
    if (!this.reverseTarget) return;
    this.receiptService.reverse(this.reverseTarget.id, this.reverseReason.trim() || undefined).subscribe({
      next: () => {
        this.reverseTarget = null;
        this.success = 'Payment reversed - the invoice balance has been restored';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to reverse payment';
        this.reverseTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.receiptService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Cannot delete';
        this.cdr.markForCheck();
      },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  statusClass(status: string): string {
    return (
      { PENDING: 'text-bg-warning', CONFIRMED: 'text-bg-primary', DEPOSITED: 'text-bg-success', FAILED: 'text-bg-danger', REVERSED: 'text-bg-secondary' }[status] ||
      'text-bg-secondary'
    );
  }
}
