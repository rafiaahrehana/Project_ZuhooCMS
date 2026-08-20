import { GatewayPaymentService } from '../../../../core/services/gateway-payment.service';
import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Invoice, InvoiceItemRequest, InvoiceRequest, CreditNote } from '../../models/finance.model';
import { InvoiceService } from '../../services/invoice.service';
import { PortalService } from '../../../portal/portal.service';
import { ClientService } from '../../../crm/services/client.service';
import { Client } from '../../../crm/models/crm.model';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-invoices',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './invoices.html',
  styleUrl: './invoices.scss',
})
export class Invoices implements OnInit {
  invoices: Invoice[] = [];
  clients: Client[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';
  selected?: Invoice | null;
  paymentAmount: number | null = null;
  statuses = ['DRAFT', 'ISSUED', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED'];
  paymentTerms = ['DUE_ON_RECEIPT', 'NET_15', 'NET_30', 'NET_45', 'NET_60', 'NET_90', 'CUSTOM'];
  currencies = ['BDT', 'USD', 'EUR', 'GBP', 'INR'];
  // The company's ledger currency - invoices in any other currency need an exchange rate.
  baseCurrency = 'BDT';

  get needsExchangeRate(): boolean {
    return !!this.form.currency && this.form.currency !== this.baseCurrency;
  }

  creditNotes: CreditNote[] = [];
  creditNoteTarget: Invoice | null = null;
  creditNoteAmount: number | null = null;
  creditNoteReason = '';
  issuingCreditNote = false;

  showForm = false;
  editing = false;
  saving = false;
  editingId: number | null = null;
  form: InvoiceRequest = this.emptyForm();
  deleteTarget: Invoice | null = null;
  cancelTarget: Invoice | null = null;

  draftingSummary = false;
  draftSummaryError = '';

  constructor(
    private invoiceService: InvoiceService,
    private clientService: ClientService,
    private portalService: PortalService,
    private gatewayPayment: GatewayPaymentService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.clientService.listActive().subscribe({
      next: (res) => { this.clients = res; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load clients'; this.cdr.markForCheck(); },
    });
    // Employees without company-settings access just keep the BDT default.
    this.portalService.getMyCompany().subscribe({
      next: (c) => { this.baseCurrency = c.baseCurrency || 'BDT'; this.cdr.markForCheck(); },
      error: () => {},
    });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    const obs = this.statusFilter
      ? this.invoiceService.listByStatus(this.statusFilter, this.page)
      : this.invoiceService.list(this.page);
    obs.subscribe({
      next: (res) => {
        this.invoices = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load invoices';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  view(invoice: Invoice): void {
    this.invoiceService.getById(invoice.id).subscribe({
      next: (i) => { this.selected = i; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load invoice details'; this.cdr.markForCheck(); },
    });
    this.invoiceService.listCreditNotes(invoice.id).subscribe({
      next: (res) => { this.creditNotes = res.content; this.cdr.markForCheck(); },
      error: () => { this.creditNotes = []; this.cdr.markForCheck(); },
    });
  }

  private emptyForm(): InvoiceRequest {
    const invoiceDate = new Date().toISOString().slice(0, 10);
    const paymentTerms = 'DUE_ON_RECEIPT';
    return {
      clientId: 0,
      invoiceDate,
      dueDate: this.calculateDueDate(invoiceDate, paymentTerms),
      items: [{ description: '', quantity: 1, unitPrice: 0 }],
      taxAmount: 0,
      taxRatePercent: undefined,
      discountAmount: 0,
      currency: 'BDT',
      paymentTerms,
      description: '',
      notes: '',
    };
  }

  // Payment terms drive the due date so it doesn't have to be worked out by hand
  // every time (NET 30 always means invoiceDate + 30 days). CUSTOM leaves it manual.
  onPaymentTermsOrDateChange(): void {
    if (!this.form.invoiceDate || !this.form.paymentTerms || this.form.paymentTerms === 'CUSTOM') return;
    this.form.dueDate = this.calculateDueDate(this.form.invoiceDate, this.form.paymentTerms);
  }

  private calculateDueDate(invoiceDate: string, paymentTerms: string): string {
    const daysByTerm: Record<string, number> = {
      DUE_ON_RECEIPT: 0, NET_15: 15, NET_30: 30, NET_45: 45, NET_60: 60, NET_90: 90,
    };
    const days = daysByTerm[paymentTerms] ?? 0;
    const date = new Date(invoiceDate);
    date.setDate(date.getDate() + days);
    return date.toISOString().slice(0, 10);
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.editing = false;
    this.editingId = null;
    this.showForm = true;
    this.error = '';
  }

  openEdit(invoice: Invoice): void {
    this.invoiceService.getById(invoice.id).subscribe({
      next: (full) => {
        this.form = {
          clientId: full.clientId ?? 0,
          invoiceDate: full.invoiceDate ?? '',
          dueDate: full.dueDate ?? '',
          items: (full.items && full.items.length
            ? full.items.map((i) => ({ description: i.description, quantity: i.quantity, unitPrice: i.unitPrice, notes: i.notes }))
            : [{ description: '', quantity: 1, unitPrice: 0 }]),
          taxAmount: full.taxAmount ?? 0,
          taxRatePercent: full.taxRatePercent,
          discountAmount: full.discountAmount ?? 0,
          currency: full.currency ?? 'BDT',
          paymentTerms: full.paymentTerms ?? 'DUE_ON_RECEIPT',
          description: full.description ?? '',
          notes: full.notes ?? '',
        };
        this.editing = true;
        this.editingId = invoice.id;
        this.showForm = true;
        this.error = '';
        this.cdr.markForCheck();
      },
      error: () => { this.error = 'Failed to load invoice for editing'; this.cdr.markForCheck(); },
    });
  }

  draftSummaryWithAi(): void {
    if (!this.editingId || this.draftingSummary) return;
    this.draftingSummary = true;
    this.draftSummaryError = '';
    this.cdr.markForCheck();
    this.invoiceService.draftSummaryWithAi(this.editingId).subscribe({
      next: (res) => {
        this.form.notes = res.summary;
        this.draftingSummary = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.draftSummaryError = err?.error?.message || 'Failed to generate summary';
        this.draftingSummary = false;
        this.cdr.markForCheck();
      },
    });
  }

  addItem(): void {
    this.form.items.push({ description: '', quantity: 1, unitPrice: 0 });
  }

  removeItem(index: number): void {
    if (this.form.items.length <= 1) return;
    this.form.items.splice(index, 1);
  }

  itemsSubtotal(): number {
    return this.form.items.reduce((sum, i) => sum + (i.quantity || 0) * (i.unitPrice || 0), 0);
  }

  // Mirrors ClientInvoice.calculateTotals() on the server: tax rate (if set)
  // recomputes the tax amount from the discounted subtotal.
  formTaxableBase(): number {
    return Math.max(0, this.itemsSubtotal() - (this.form.discountAmount || 0));
  }

  formTaxAmount(): number {
    if (this.form.taxRatePercent != null) {
      return this.formTaxableBase() * (this.form.taxRatePercent / 100);
    }
    return this.form.taxAmount || 0;
  }

  formTotal(): number {
    return this.formTaxableBase() + this.formTaxAmount();
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const payload: InvoiceRequest = {
      ...this.form,
      // The backend forces rate 1 for base-currency invoices; don't send a stale one.
      exchangeRate: this.needsExchangeRate ? this.form.exchangeRate : undefined,
      items: this.form.items.filter((i) => i.description && i.quantity > 0),
    };
    const obs = this.editing && this.editingId
      ? this.invoiceService.update(this.editingId, payload)
      : this.invoiceService.create(payload);
    obs.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.editing ? 'Invoice updated' : 'Invoice created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save invoice';
        this.cdr.markForCheck();
      },
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.invoiceService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        if (this.selected?.id === this.deleteTarget!.id) this.selected = undefined;
        this.deleteTarget = null;
        this.success = 'Invoice deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete invoice';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  confirmCancel(): void {
    if (!this.cancelTarget) return;
    this.invoiceService.cancel(this.cancelTarget.id).subscribe({
      next: () => {
        if (this.selected?.id === this.cancelTarget!.id) this.selected = undefined;
        this.cancelTarget = null;
        this.success = 'Invoice cancelled';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to cancel invoice';
        this.cancelTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  send(invoice: Invoice): void {
    this.invoiceService.send(invoice.id).subscribe({
      next: () => {
        this.success = 'Invoice sent';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  markPaid(invoice: Invoice): void {
    const amount = invoice.balanceAmount ?? invoice.totalAmount;
    this.invoiceService.markPaid(invoice.id, amount).subscribe({
      next: () => {
        this.success = 'Marked as paid';
        this.cdr.markForCheck();
        this.load();
        if (this.selected?.id === invoice.id) this.view(invoice);
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  recordPayment(): void {
    if (!this.selected || !this.paymentAmount) return;
    this.invoiceService
      .recordPayment(this.selected.id, this.paymentAmount)
      .subscribe({
        next: () => {
          this.success = 'Payment recorded';
          this.paymentAmount = null;
          this.cdr.markForCheck();
          this.view(this.selected!);
          this.load();
        },
        error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
      });
  }

  // SSLCommerz checkout for the remaining balance; on validated success the
  // backend records the payment (recordPaymentForCompany)
  payOnline(): void {
    if (!this.selected) return;
    const remaining = (this.selected.totalAmount ?? 0) - (this.selected.paidAmount ?? 0);
    if (remaining <= 0) {
      this.error = 'This invoice has no outstanding balance';
      this.cdr.markForCheck();
      return;
    }
    this.gatewayPayment.redirectToGateway('INVOICE', this.selected.id, remaining,
      (msg) => { this.error = msg; this.cdr.markForCheck(); });
  }

  openCreditNote(invoice: Invoice): void {
    this.creditNoteTarget = invoice;
    this.creditNoteAmount = null;
    this.creditNoteReason = '';
  }

  confirmCreditNote(): void {
    if (!this.creditNoteTarget || !this.creditNoteAmount) return;
    this.issuingCreditNote = true;
    this.invoiceService.issueCreditNote({
      clientInvoiceId: this.creditNoteTarget.id,
      amount: this.creditNoteAmount,
      reason: this.creditNoteReason,
    }).subscribe({
      next: () => {
        this.issuingCreditNote = false;
        this.success = 'Credit note issued';
        const invoiceId = this.creditNoteTarget!.id;
        this.creditNoteTarget = null;
        this.cdr.markForCheck();
        this.load();
        if (this.selected?.id === invoiceId) this.view(this.selected);
      },
      error: (err) => {
        this.issuingCreditNote = false;
        this.error = err?.error?.message || 'Failed to issue credit note';
        this.cdr.markForCheck();
      },
    });
  }

  downloadPdf(invoice: Invoice): void {
    this.invoiceService.downloadPdf(invoice.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${invoice.invoiceNumber}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.error = 'Failed to download invoice'; this.cdr.markForCheck(); },
    });
  }

  statusClass(s: string): string {
    return (
      {
        PAID: 'text-bg-success',
        ISSUED: 'text-bg-primary',
        OVERDUE: 'text-bg-danger',
        CANCELLED: 'text-bg-secondary',
        DRAFT: 'text-bg-light',
        PARTIALLY_PAID: 'text-bg-warning',
      }[s] || 'text-bg-secondary'
    );
  }
  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
