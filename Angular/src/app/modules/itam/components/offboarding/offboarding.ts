import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OffboardingChecklist, SoftwareLicenseSeat } from '../../models/itam.model';
import { OffboardingService } from '../../services/offboarding.service';
import { SoftwareService } from '../../services/software.service';
import { HrAssetService } from '../../../hrm/services/hr-asset.service';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { Employee, HrAsset } from '../../../hrm/models/hrm.model';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-offboarding',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './offboarding.html',
})
export class Offboarding implements OnInit {
  checklists: OffboardingChecklist[] = [];
  pending: OffboardingChecklist[] = [];
  employees: Employee[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';

  showForm = false;
  newEmployeeId?: number;
  newNotes = '';

  selected: OffboardingChecklist | null = null;
  deleteTarget: OffboardingChecklist | null = null;

  assignedAssets: HrAsset[] = [];
  assignedLicenses: SoftwareLicenseSeat[] = [];
  assignedLoading = false;

  constructor(
    private offboardingService: OffboardingService,
    private assetService: HrAssetService,
    private softwareService: SoftwareService,
    private employeeService: EmployeeService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadPending();
    this.employeeService.list(0, 500).subscribe({ next: (res) => { this.employees = res.content; this.cdr.markForCheck(); } });
  }

  load(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.offboardingService.list(this.page).subscribe({
      next: (res) => {
        this.checklists = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load offboarding checklists';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadPending(): void {
    this.offboardingService.getPending().subscribe({ next: (res) => { this.pending = res; this.cdr.markForCheck(); } });
  }

  startOffboarding(): void {
    if (!this.newEmployeeId) return;
    this.offboardingService.create({ employeeId: this.newEmployeeId, notes: this.newNotes }).subscribe({
      next: () => {
        this.showForm = false;
        this.newEmployeeId = undefined;
        this.newNotes = '';
        this.success = 'Offboarding checklist created';
        this.cdr.markForCheck();
        this.load();
        this.loadPending();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to start offboarding'; this.cdr.markForCheck(); },
    });
  }

  view(c: OffboardingChecklist): void {
    this.selected = c;
    this.assignedAssets = [];
    this.assignedLicenses = [];
    this.assignedLoading = true;
    this.cdr.markForCheck();

    this.assetService.listForEmployee(c.employeeId).subscribe({
      next: (res) => { this.assignedAssets = res; this.assignedLoading = false; this.cdr.markForCheck(); },
      error: () => { this.assignedLoading = false; this.cdr.markForCheck(); },
    });
    this.softwareService.getLicensesForEmployee(c.employeeId).subscribe({
      next: (res) => { this.assignedLicenses = res; this.cdr.markForCheck(); },
    });
  }

  refreshSelected(): void {
    if (!this.selected) return;
    this.offboardingService.getById(this.selected.id).subscribe({ next: (c) => { this.selected = c; this.cdr.markForCheck(); } });
  }

  returnAsset(asset: HrAsset): void {
    if (!this.selected) return;
    this.assetService.unassign(asset.id).subscribe({
      next: () => {
        this.success = asset.name + ' returned';
        this.cdr.markForCheck();
        this.view(this.selected!);
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to return asset'; this.cdr.markForCheck(); },
    });
  }

  revokeLicenseSeat(seat: SoftwareLicenseSeat): void {
    if (!this.selected) return;
    this.softwareService.releaseSeat(seat.licenseId, seat.employeeId).subscribe({
      next: () => {
        this.success = seat.softwareName + ' seat revoked';
        this.cdr.markForCheck();
        this.view(this.selected!);
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to revoke seat'; this.cdr.markForCheck(); },
    });
  }

  markHardware(): void {
    if (!this.selected) return;
    this.offboardingService.markHardwareCollected(this.selected.id).subscribe({
      next: () => {
        this.refreshSelected();
        this.load();
        this.loadPending();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  markLicenses(): void {
    if (!this.selected) return;
    this.offboardingService.markLicensesRevoked(this.selected.id).subscribe({
      next: () => {
        this.refreshSelected();
        this.load();
        this.loadPending();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  markAccess(): void {
    if (!this.selected) return;
    this.offboardingService.markAccessRevoked(this.selected.id).subscribe({
      next: () => {
        this.refreshSelected();
        this.load();
        this.loadPending();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  markData(): void {
    if (!this.selected) return;
    this.offboardingService.markDataHandedOver(this.selected.id).subscribe({
      next: () => {
        this.refreshSelected();
        this.load();
        this.loadPending();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  markExitInterview(): void {
    if (!this.selected) return;
    this.offboardingService.markExitInterviewCompleted(this.selected.id).subscribe({
      next: () => {
        this.refreshSelected();
        this.load();
        this.loadPending();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed'; this.cdr.markForCheck(); },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.offboardingService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        if (this.selected?.id === this.deleteTarget?.id) this.selected = null;
        this.deleteTarget = null;
        this.success = 'Checklist deleted';
        this.cdr.markForCheck();
        this.load();
        this.loadPending();
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
}
