import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { InvoiceService, RefundRequest } from '../../services/invoice.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-refunds',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './refunds.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Refunds implements OnInit {
  refunds: RefundRequest[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = 'PROCESSED';
  statuses = ['REQUESTED', 'PROCESSED', 'REJECTED'];

  processTarget: RefundRequest | null = null;
  rejectTarget: RefundRequest | null = null;
  rejectReason = '';

  constructor(
    private invoiceService: InvoiceService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.invoiceService.listRefunds(this.statusFilter || undefined, this.page).subscribe({
      next: (res) => {
        this.refunds = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load refund requests';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  setStatusFilter(status: string): void {
    this.statusFilter = status;
    this.page = 0;
    this.load();
  }

  confirmProcess(): void {
    if (!this.processTarget) return;
    this.invoiceService.processRefund(this.processTarget.id).subscribe({
      next: () => {
        this.success = 'Refund processed';
        this.processTarget = null;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to process refund';
        this.processTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  confirmReject(): void {
    if (!this.rejectTarget) return;
    this.invoiceService.rejectRefund(this.rejectTarget.id, this.rejectReason.trim() || undefined).subscribe({
      next: () => {
        this.success = 'Refund rejected';
        this.rejectTarget = null;
        this.rejectReason = '';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to reject refund';
        this.rejectTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  statusClass(status: string): string {
    return (
      {
        REQUESTED: 'text-bg-warning',
        PROCESSED: 'text-bg-success',
        REJECTED: 'text-bg-danger',
      }[status] || 'text-bg-secondary'
    );
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
