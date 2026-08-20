import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LeaveBalance,
  LeaveBalanceRequest,
  Employee,
  LEAVE_TYPES,
} from '../../models/hrm.model';
import { LeaveBalanceService } from '../../services/leave-balance.service';
import { EmployeeService } from '../../services/employee.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-leave-balances',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './leave-balances.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaveBalances implements OnInit {
  balances: LeaveBalance[] = [];
  employees: Employee[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  year = new Date().getFullYear();

  showForm = false;
  isEdit = false;
  selectedId: number | null = null;
  form: LeaveBalanceRequest = this.emptyForm();

  deleteTarget: LeaveBalance | null = null;

  leaveTypes = LEAVE_TYPES;

  constructor(
    private balanceService: LeaveBalanceService,
    private employeeService: EmployeeService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.loadEmployees();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.balanceService.list(this.page, 20, this.year).subscribe({
      next: (res) => {
        this.balances = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load leave balances';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadEmployees(): void {
    this.employeeService.list(0, 100).subscribe({
      next: (res) => { this.employees = res.content; this.cdr.markForCheck(); },
      error: () => { this.employees = []; this.cdr.markForCheck(); }
    });
  }

  onYearChange(): void {
    this.page = 0;
    this.load();
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.isEdit = false;
    this.showForm = true;
  }

  openEdit(b: LeaveBalance): void {
    this.form = {
      employeeId: b.employeeId!,
      leaveType: b.leaveType,
      year: b.year,
      totalDays: b.entitledDays,
    };
    this.selectedId = b.id;
    this.isEdit = true;
    this.showForm = true;
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const request = this.isEdit && this.selectedId
      ? this.balanceService.update(this.selectedId, this.form)
      : this.balanceService.create(this.form);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.isEdit ? 'Leave balance updated' : 'Leave balance created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save leave balance';
        this.cdr.markForCheck();
      }
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.balanceService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Leave balance deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Failed to delete leave balance';
        this.cdr.markForCheck();
      }
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  private emptyForm(): LeaveBalanceRequest {
    return {
      employeeId: this.employees[0]?.id ?? 0,
      leaveType: 'ANNUAL',
      year: this.year,
      totalDays: 0,
    };
  }
}
