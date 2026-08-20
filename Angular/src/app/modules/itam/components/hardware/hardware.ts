import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Employee, HrAsset, HrAssetRequest } from '../../../hrm/models/hrm.model';
import { HrAssetService } from '../../../hrm/services/hr-asset.service';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

// Hardware reuses the shared hrm/asset backend (AssetController, "/api/hr/assets") rather
// than a dedicated ITAM hardware controller (none exists). AssetResponse/AssetRequest do
// expose the IT-hardware-specific columns (ipAddress, macAddress, processorModel, ramSize,
// storageSize, operatingSystem, assetTag, brand, model, warrantyExpiry). "Category" is used
// as a free-text way to mark hardware type (Laptop, Monitor, etc.) since there is no
// dedicated hardware-type enum on the backend.
@Component({
  selector: 'app-hardware',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './hardware.html',
})
export class Hardware implements OnInit {
  assets: HrAsset[] = [];
  employees: Employee[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  showForm = false;
  editId: number | null = null;
  form: HrAssetRequest = { name: '' };
  deleteTarget: HrAsset | null = null;
  assignTarget: HrAsset | null = null;
  assignEmployeeId?: number;
  disposeTarget: HrAsset | null = null;
  disposeReason = '';

  categories = ['Laptop', 'Desktop', 'Monitor', 'Printer', 'Phone', 'Tablet', 'Server', 'Networking', 'Other'];
  today = new Date().toISOString().slice(0, 10);

  constructor(
    private assetService: HrAssetService,
    private employeeService: EmployeeService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.employeeService.list(0, 500).subscribe({ next: (res) => { this.employees = res.content; this.cdr.markForCheck(); } });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.assetService.list(this.page).subscribe({
      next: (res) => {
        this.assets = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load hardware assets';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreate(): void {
    this.editId = null;
    this.form = { name: '' };
    this.showForm = true;
  }

  openEdit(a: HrAsset): void {
    this.editId = a.id;
    this.form = {
      name: a.name,
      category: a.category,
      serialNumber: a.serialNumber,
      description: a.description,
      purchaseDate: a.purchaseDate,
      purchaseCost: a.purchaseCost,
      notes: a.notes,
      assetTag: a.assetTag,
      brand: a.brand,
      model: a.model,
      ipAddress: a.ipAddress,
      macAddress: a.macAddress,
      processorModel: a.processorModel,
      ramSize: a.ramSize,
      storageSize: a.storageSize,
      operatingSystem: a.operatingSystem,
      warrantyExpiry: a.warrantyExpiry,
    };
    this.showForm = true;
  }

  save(): void {
    const op = this.editId
      ? this.assetService.update(this.editId, this.form)
      : this.assetService.create(this.form);
    op.subscribe({
      next: () => {
        this.showForm = false;
        this.success = 'Saved';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  doAssign(): void {
    if (!this.assignTarget || !this.assignEmployeeId) return;
    this.assetService.assign(this.assignTarget.id, this.assignEmployeeId).subscribe({
      next: () => {
        this.assignTarget = null;
        this.success = 'Assigned';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.assignTarget = null;
        this.error = err?.error?.message || 'Failed';
        this.cdr.markForCheck();
      },
    });
  }

  unassign(a: HrAsset): void {
    this.assetService.unassign(a.id).subscribe({
      next: () => {
        this.success = 'Returned';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  toggleMaintenance(a: HrAsset): void {
    this.assetService.setMaintenance(a.id, a.status !== 'UNDER_MAINTENANCE').subscribe({
      next: () => {
        this.success = a.status === 'UNDER_MAINTENANCE' ? 'Back in service' : 'Marked under maintenance';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  doDispose(): void {
    if (!this.disposeTarget) return;
    this.assetService.dispose(this.disposeTarget.id, this.disposeReason || undefined).subscribe({
      next: () => {
        this.disposeTarget = null;
        this.disposeReason = '';
        this.success = 'Disposed';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed';
        this.disposeTarget = null;
        this.cdr.markForCheck();
      },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.assetService.delete(this.deleteTarget.id).subscribe({
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

  statusClass(s: string): string {
    return (
      { AVAILABLE: 'text-bg-success', ASSIGNED: 'text-bg-primary', UNDER_MAINTENANCE: 'text-bg-warning', DISPOSED: 'text-bg-secondary' }[s] ||
      'text-bg-light'
    );
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
