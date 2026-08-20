import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Shift, ShiftRequest, SHIFT_TYPES } from '../../models/hrm.model';
import { ShiftService } from '../../services/shift.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-shifts',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  templateUrl: './shifts.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shifts implements OnInit {
  shifts: Shift[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  saving = false;
  error = '';
  success = '';

  showForm = false;
  isEdit = false;
  selectedId: number | null = null;
  form: ShiftRequest = this.emptyForm();

  deleteTarget: Shift | null = null;

  shiftTypes = SHIFT_TYPES;

  constructor(private shiftService: ShiftService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.shiftService.list(this.page, 20).subscribe({
      next: (res) => {
        this.shifts = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load shifts';
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

  openEdit(s: Shift): void {
    this.form = {
      name: s.name,
      shiftType: s.shiftType,
      startTime: s.startTime,
      endTime: s.endTime,
      gracePeriodMinutes: s.gracePeriodMinutes,
      weeklyOffDays: s.weeklyOffDays,
      flexible: s.flexible,
      nightShift: s.nightShift,
      description: s.description,
      notes: s.notes,
    };
    this.selectedId = s.id;
    this.isEdit = true;
    this.showForm = true;
  }

  save(): void {
    this.saving = true;
    this.error = '';
    const payload = this.cleanPayload();
    const request = this.isEdit && this.selectedId
      ? this.shiftService.update(this.selectedId, payload)
      : this.shiftService.create(payload);

    request.subscribe({
      next: () => {
        this.saving = false;
        this.showForm = false;
        this.success = this.isEdit ? 'Shift updated' : 'Shift created';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to save shift';
        this.cdr.markForCheck();
      }
    });
  }

  toggleActive(s: Shift): void {
    this.shiftService.toggle(s.id).subscribe({
      next: () => {
        this.success = 'Shift status updated';
        this.cdr.markForCheck();
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to toggle status';
        this.cdr.markForCheck();
      }
    });
  }

  confirmDelete(): void {
    if (!this.deleteTarget) return;
    this.shiftService.delete(this.deleteTarget.id).subscribe({
      next: () => {
        this.deleteTarget = null;
        this.success = 'Shift deleted';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.deleteTarget = null;
        this.error = 'Failed to delete shift';
        this.cdr.markForCheck();
      }
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  // Top-accent color per shift type.
  shiftColor(type?: string): string {
    switch (type) {
      case 'MORNING': return '#F59E0B';
      case 'AFTERNOON': return '#0EA5E9';
      case 'FULL_DAY': return '#16A34A';
      case 'EVENING': return '#8B5CF6';
      case 'NIGHT': return '#1E1B4B';
      case 'FLEXIBLE': return '#14B8A6';
      default: return '#6B46FF';
    }
  }

  private emptyForm(): ShiftRequest {
    return {
      name: '',
      shiftType: 'FULL_DAY',
      startTime: '09:00',
      endTime: '18:00',
      gracePeriodMinutes: 15,
      weeklyOffDays: 'Saturday, Sunday',
      flexible: false,
      nightShift: false,
    };
  }

  private cleanPayload(): ShiftRequest {
    const payload: any = { ...this.form };
    return payload;
  }
}
