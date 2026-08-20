import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SoftwareLicense, SoftwareLicenseSeat } from '../../models/itam.model';
import { SoftwareService } from '../../services/software.service';
import { Employee } from '../../../hrm/models/hrm.model';
import { EmployeeService } from '../../../hrm/services/employee.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-software',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './software.html',
  styleUrl: './software.scss',
})
export class Software implements OnInit {
  licenses: SoftwareLicense[] = [];
  employees: Employee[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  statusFilter = '';
  showExpiring = false;
  showForm = false;
  editId: number | null = null;
  form: any = {};
  deleteTarget: SoftwareLicense | null = null;
  statuses = ['ACTIVE', 'EXPIRED', 'EXPIRING_SOON', 'SUSPENDED', 'REVOKED'];
  licenseTypes = ['PERPETUAL', 'SUBSCRIPTION', 'TRIAL', 'OPEN_SOURCE'];
  renewalTypes = ['ANNUAL', 'MONTHLY', 'BIENNIAL', 'PERPETUAL'];

  seatsTarget: SoftwareLicense | null = null;
  seatHolders: SoftwareLicenseSeat[] = [];
  seatsLoading = false;
  newSeatEmployeeId: number | null = null;

  constructor(
    private softwareService: SoftwareService,
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
    if (this.showExpiring) {
      this.softwareService.expiring().subscribe({
        next: (res) => {
          this.licenses = res;
          this.totalPages = 1;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Failed to load licenses';
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
    } else {
      const obs = this.statusFilter
        ? this.softwareService.listByStatus(this.statusFilter, this.page)
        : this.softwareService.list(this.page);
      obs.subscribe({
        next: (res) => {
          this.licenses = res.content;
          this.totalPages = res.totalPages;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Failed to load licenses';
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
    }
  }

  save(): void {
    const op = this.editId
      ? this.softwareService.update(this.editId, this.form)
      : this.softwareService.create(this.form);
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

  openSeats(l: SoftwareLicense): void {
    this.seatsTarget = l;
    this.newSeatEmployeeId = null;
    this.error = '';
    this.loadSeatHolders();
  }

  loadSeatHolders(): void {
    if (!this.seatsTarget) return;
    this.seatsLoading = true;
    this.cdr.markForCheck();
    this.softwareService.getSeatHolders(this.seatsTarget.id).subscribe({
      next: (res) => { this.seatHolders = res; this.seatsLoading = false; this.cdr.markForCheck(); },
      error: () => { this.seatsLoading = false; this.cdr.markForCheck(); },
    });
  }

  assignSeat(): void {
    if (!this.seatsTarget || !this.newSeatEmployeeId) return;
    this.softwareService.assignSeat(this.seatsTarget.id, this.newSeatEmployeeId).subscribe({
      next: () => {
        this.newSeatEmployeeId = null;
        this.success = 'Seat assigned';
        this.cdr.markForCheck();
        this.loadSeatHolders();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to assign seat'; this.cdr.markForCheck(); },
    });
  }

  releaseSeat(seat: SoftwareLicenseSeat): void {
    if (!this.seatsTarget) return;
    this.softwareService.releaseSeat(this.seatsTarget.id, seat.employeeId).subscribe({
      next: () => {
        this.success = 'Seat released';
        this.cdr.markForCheck();
        this.loadSeatHolders();
        this.load();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to release seat'; this.cdr.markForCheck(); },
    });
  }

  doDelete(): void {
    if (!this.deleteTarget) return;
    this.softwareService.delete(this.deleteTarget.id).subscribe({
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
      {
        ACTIVE: 'text-bg-success',
        EXPIRED: 'text-bg-danger',
        EXPIRING_SOON: 'text-bg-warning',
        SUSPENDED: 'text-bg-secondary',
        REVOKED: 'text-bg-dark',
      }[s] || 'text-bg-light'
    );
  }
  goToPage(p: number): void {
    this.page = p;
    this.load();
  }
}
