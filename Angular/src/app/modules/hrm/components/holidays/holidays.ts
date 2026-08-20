import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Holiday, HolidayRequest, HOLIDAY_TYPES } from '../../models/hrm.model';
import { HolidayService } from '../../services/holiday.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-holidays',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './holidays.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Holidays implements OnInit {
  holidays: Holiday[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  isEdit = false;
  selectedId: number | null = null;
  form: HolidayRequest = this.emptyForm();

  deleteTarget: Holiday | null = null;

  holidayTypes = HOLIDAY_TYPES;

  constructor(
    private holidayService: HolidayService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.holidayService.list(this.page, 50).subscribe({
      next: (res) => {
        this.holidays = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load holidays';
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  openCreate(): void {
    this.form = this.emptyForm();
    this.isEdit = false;
    this.showForm = true;
  }

  openEdit(h: Holiday): void {
    this.form = {
      name: h.name,
      holidayDate: h.holidayDate,
      holidayType: h.holidayType,
      description: h.description,
    };
    this.selectedId = h.id;
    this.isEdit = true;
    this.showForm = true;
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const payload = this.cleanPayload();
    const request = this.isEdit && this.selectedId
      ? this.holidayService.update(this.selectedId, payload)
      : this.holidayService.create(payload);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.isEdit ? 'Holiday updated successfully' : 'Holiday created successfully';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save holiday';
        this.cdr.markForCheck();
      }
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.holidayService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Holiday deleted successfully';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Failed to delete holiday';
        this.cdr.markForCheck();
      }
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  private emptyForm(): HolidayRequest {
    return {
      name: '',
      holidayDate: '',
      holidayType: 'COMPANY',
      description: '',
    };
  }

  private cleanPayload(): HolidayRequest {
    return { ...this.form };
  }
}
