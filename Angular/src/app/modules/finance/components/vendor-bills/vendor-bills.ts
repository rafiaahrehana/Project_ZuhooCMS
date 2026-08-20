import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Vendor, VendorBill, VendorBillRequest, VendorBillStatus, ChartOfAccount } from '../../models/finance.model';
import { VendorService, VendorBillService } from '../../services/vendor.service';
import { CoaService } from '../../services/coa.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-vendor-bills',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './vendor-bills.html',
})
export class VendorBills implements OnInit {
  bills: VendorBill[] = [];
  vendors: Vendor[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter: VendorBillStatus | '' = '';
  statuses: VendorBillStatus[] = ['DRAFT', 'APPROVED', 'PARTIALLY_PAID', 'OVERDUE', 'PAID', 'CANCELLED'];

  showForm = false;
  saving = false;
  form: VendorBillRequest = this.emptyForm();

  cancelTarget: VendorBill | null = null;
  payTarget: VendorBill | null = null;
  payAmount: number | null = null;
  paying = false;
  approvingId: number | null = null;

  // EXPENSE-type COA accounts for the "posts to" select
  expenseAccounts: ChartOfAccount[] = [];

  constructor(
    private billService: VendorBillService,
    private vendorService: VendorService,
    private coaService: CoaService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.vendorService.listActive().subscribe({
      next: (res) => { this.vendors = res; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load vendors'; this.cdr.markForCheck(); },
    });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.billService.list(this.statusFilter, undefined, this.page).subscribe({
      next: (res) => {
        this.bills = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load vendor bills';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  private emptyForm(): VendorBillRequest {
    return {
      vendorId: null,
      billDate: new Date().toISOString().slice(0, 10),
      dueDate: '',
      subtotal: 0,
      taxAmount: 0,
      vendorReference: '',
      description: '',
      expenseAccountId: null,
    };
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.showForm = true;
    this.error = '';
    if (!this.expenseAccounts.length) {
      this.coaService.list(0, 200).subscribe({
        next: (res) => {
          this.expenseAccounts = res.content.filter((a) => a.type === 'EXPENSE' && a.active);
          this.cdr.markForCheck();
        },
        error: () => {},
      });
    }
  }

  formTotal(): number {
    return (this.form.subtotal || 0) + (this.form.taxAmount || 0);
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.billService.create(this.form).subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = 'Vendor bill created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to create vendor bill';
        this.cdr.markForCheck();
      },
    });
  }

  approve(bill: VendorBill): void {
    if (this.approvingId) return;
    this.approvingId = bill.id;
    this.error = '';
    this.cdr.markForCheck();
    this.billService.approve(bill.id).subscribe({
      next: () => {
        this.approvingId = null;
        this.success = `Bill ${bill.billNumber} approved`;
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.approvingId = null;
        this.error = err?.error?.message || 'Failed to approve bill';
        this.cdr.markForCheck();
      },
    });
  }

  openPay(bill: VendorBill): void {
    this.payTarget = bill;
    this.payAmount = bill.balanceAmount;
    this.error = '';
  }

  confirmPay(): void {
    if (!this.payTarget || !this.payAmount) return;
    this.paying = true;
    this.error = '';
    this.billService.pay(this.payTarget.id, this.payAmount).subscribe({
      next: () => {
        this.paying = false;
        this.payTarget = null;
        this.payAmount = null;
        this.success = 'Payment recorded';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.paying = false;
        this.error = err?.error?.message || 'Failed to record payment';
        this.cdr.markForCheck();
      },
    });
  }

  confirmCancel(): void {
    if (!this.cancelTarget) return;
    this.billService.cancel(this.cancelTarget.id).subscribe({
      next: () => {
        this.cancelTarget = null;
        this.success = 'Bill cancelled';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to cancel bill';
        this.cancelTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  statusClass(s: string): string {
    return (
      {
        DRAFT: 'text-bg-secondary',
        APPROVED: 'text-bg-info',
        PARTIALLY_PAID: 'text-bg-warning',
        OVERDUE: 'text-bg-danger',
        PAID: 'text-bg-success',
        CANCELLED: 'text-bg-secondary',
      }[s] || 'text-bg-secondary'
    );
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
