import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Vendor, VendorRequest } from '../../models/finance.model';
import { VendorService } from '../../services/vendor.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-vendors',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './vendors.html',
})
export class Vendors implements OnInit {
  vendors: Vendor[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  search = '';

  showForm = false;
  editing = false;
  saving = false;
  editingId: number | null = null;
  form: VendorRequest = this.emptyForm();
  deleteTarget: Vendor | null = null;

  constructor(private vendorService: VendorService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.vendorService.list(this.search || undefined, this.page).subscribe({
      next: (res) => {
        this.vendors = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load vendors';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  onSearch(): void {
    this.page = 0;
    this.load();
  }

  private emptyForm(): VendorRequest {
    return {
      name: '',
      contactPerson: '',
      email: '',
      phone: '',
      taxId: '',
      address: '',
      paymentTerms: '',
      notes: '',
    };
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.editing = false;
    this.editingId = null;
    this.showForm = true;
    this.error = '';
  }

  openEdit(vendor: Vendor): void {
    this.form = {
      name: vendor.name,
      contactPerson: vendor.contactPerson ?? '',
      email: vendor.email ?? '',
      phone: vendor.phone ?? '',
      taxId: vendor.taxId ?? '',
      address: vendor.address ?? '',
      paymentTerms: vendor.paymentTerms ?? '',
      notes: vendor.notes ?? '',
    };
    this.editing = true;
    this.editingId = vendor.id;
    this.showForm = true;
    this.error = '';
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const obs = this.editing && this.editingId
      ? this.vendorService.update(this.editingId, this.form)
      : this.vendorService.create(this.form);
    obs.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.editing ? 'Vendor updated' : 'Vendor created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save vendor';
        this.cdr.markForCheck();
      },
    });
  }

  toggle(vendor: Vendor): void {
    this.vendorService.toggle(vendor.id).subscribe({
      next: () => {
        this.success = vendor.active ? 'Vendor disabled' : 'Vendor enabled';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update vendor';
        this.cdr.markForCheck();
      },
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.vendorService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Vendor deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete vendor';
        this.deleteTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
