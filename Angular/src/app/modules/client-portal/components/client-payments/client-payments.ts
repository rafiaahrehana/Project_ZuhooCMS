import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { InvoiceService } from '../../../finance/services/invoice.service';
import { PaymentReceiptService } from '../../../finance/services/payment-receipt.service';
import { GatewayPaymentService } from '../../../../core/services/gateway-payment.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
// Real ClientInvoiceResponse shape from the backend - the shared Invoice model in
// finance.model.ts is out of sync with it (claims outstandingAmount/discountAmount,
// which don't exist server-side; the real field is balanceAmount). Typed locally
// here rather than propagating that mismatch.
interface MyInvoice {
  id: number;
  invoiceNumber: string;
  invoiceDate?: string;
  dueDate?: string;
  totalAmount: number;
  paidAmount: number;
  balanceAmount: number;
  status: string;
  description?: string;
  serviceRequestId?: number;
  serviceRequestTitle?: string;
  // Only set while a refund is REQUESTED/REJECTED - once PROCESSED, status above
  // already reads REFUNDED so this is left unset to avoid a redundant badge.
  refundStatus?: string;
}

interface MyReceipt {
  id: number;
  receiptNumber: string;
  invoiceId?: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  status: string;
}

type Tab = 'invoices' | 'receipts';

@Component({
  selector: 'app-client-payments',
  imports: [BosCurrencyPipe, CommonModule, RouterLink, Loader, EmptyState],
  templateUrl: './client-payments.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientPayments implements OnInit {
  tab: Tab = 'invoices';
  invoices: MyInvoice[] = [];
  receipts: MyReceipt[] = [];
  loading = false;
  error = '';
  payingId: number | null = null;

  constructor(
    private invoiceService: InvoiceService,
    private receiptService: PaymentReceiptService,
    private gatewayPayment: GatewayPaymentService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadInvoices();
  }

  setTab(tab: Tab): void {
    this.tab = tab;
    if (tab === 'invoices' && !this.invoices.length) this.loadInvoices();
    if (tab === 'receipts' && !this.receipts.length) this.loadReceipts();
  }

  loadInvoices(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.invoiceService.my().subscribe({
      next: (res: any) => {
        this.invoices = res.content;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load your invoices';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadReceipts(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.receiptService.my().subscribe({
      next: (res: any) => {
        this.receipts = res.content;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load your payment history';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  downloadInvoice(inv: MyInvoice): void {
    this.invoiceService.downloadPdf(inv.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${inv.invoiceNumber}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => { this.error = 'Failed to download invoice'; this.cdr.markForCheck(); },
    });
  }

  canPay(inv: MyInvoice): boolean {
    return inv.balanceAmount > 0 && inv.status !== 'CANCELLED' && inv.status !== 'DRAFT';
  }

  // SSLCommerz checkout for the outstanding balance - same GatewayPaymentService
  // the staff-facing invoices page uses. SslCommerzServiceImpl.validateTarget()
  // enforces server-side that a CLIENT can only pay their own invoice.
  payOnline(inv: MyInvoice): void {
    if (!this.canPay(inv) || this.payingId) return;
    this.payingId = inv.id;
    this.error = '';
    this.cdr.markForCheck();
    this.gatewayPayment.redirectToGateway('INVOICE', inv.id, inv.balanceAmount, (msg) => {
      this.payingId = null;
      this.error = msg;
      this.cdr.markForCheck();
    });
  }

  statusClass(status: string): string {
    return {
      PAID: 'text-bg-success',
      SENT: 'text-bg-info',
      OVERDUE: 'text-bg-danger',
      DRAFT: 'text-bg-secondary',
      CANCELLED: 'text-bg-dark',
      REFUNDED: 'text-bg-warning',
      CONFIRMED: 'text-bg-success',
      PENDING: 'text-bg-warning',
      DEPOSITED: 'text-bg-info',
    }[status] || 'text-bg-secondary';
  }

  refundStatusClass(status: string): string {
    return (
      {
        REQUESTED: 'text-bg-warning',
        REJECTED: 'text-bg-danger',
      }[status] || 'text-bg-secondary'
    );
  }
}
