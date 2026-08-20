import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AttendanceRecord } from '../../models/attendance.model';
import { AttendanceService } from '../../services/attendance.service';
import { Loader } from '../../../../shared/components/loader/loader';

@Component({
  selector: 'app-check-in-out',
  imports: [CommonModule, FormsModule, Loader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './check-in-out.html',
})
export class CheckInOut implements OnInit {
  todayRecord?: AttendanceRecord;
  recentRecords: AttendanceRecord[] = [];
  loading = false;
  error = '';
  success = '';
  notes = '';
  location = '';

  constructor(private attendanceService: AttendanceService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadMyToday();
    this.loadRecent();
  }

  loadMyToday(): void {
    this.attendanceService.myToday().subscribe({
      next: (rec) => {
        this.todayRecord = rec || undefined;
        this.cdr.markForCheck();
      },
      error: () => {
        this.todayRecord = undefined;
        this.cdr.markForCheck();
      }
    });
  }

  get consolidatedRecentRecords(): AttendanceRecord[] {
    const map = new Map<string, AttendanceRecord>();
    for (const r of this.recentRecords) {
      const key = `${r.employeeId || r.employeeName}_${r.attendanceDate}`;
      if (!map.has(key)) {
        map.set(key, { ...r });
      } else {
        const existing = map.get(key)!;
        if (!existing.checkInTime && r.checkInTime) {
          existing.checkInTime = r.checkInTime;
        }
        if (!existing.checkOutTime && r.checkOutTime) {
          existing.checkOutTime = r.checkOutTime;
        }
        if (existing.status === 'ABSENT' && r.status !== 'ABSENT') {
          existing.status = r.status;
        }
        if (r.isLate) {
          existing.isLate = true;
          existing.lateMinutes = r.lateMinutes;
        }
        if (r.approved) {
          existing.approved = true;
        }
        if (existing.checkInTime && existing.checkOutTime) {
          existing.totalWorkingHours = this.calcHours(existing.checkInTime, existing.checkOutTime);
        }
      }
    }
    return Array.from(map.values());
  }

  private calcHours(inTime: string, outTime: string): number {
    try {
      const [h1, m1] = inTime.split(':').map(Number);
      const [h2, m2] = outTime.split(':').map(Number);
      const mins = (h2 * 60 + m2) - (h1 * 60 + m1);
      return mins > 0 ? Number((mins / 60).toFixed(2)) : 0;
    } catch {
      return 0;
    }
  }

  loadRecent(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.attendanceService.myRecords(0, 20).subscribe({
      next: (res) => {
        this.recentRecords = res.content || [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load attendance';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  checkIn(): void {
    this.error = '';
    const now = new Date();
    const timeStr = now.toTimeString().split(' ')[0]; // HH:mm:ss
    const payload = {
      checkInTime: timeStr,
      method: 'MANUAL',
      notes: this.notes,
      location: this.location
    };
    this.attendanceService
      .checkIn(payload)
      .subscribe({
        next: (r) => {
          this.todayRecord = r;
          this.success = 'Checked in successfully';
          this.notes = '';
          this.cdr.markForCheck();
          this.loadMyToday();
          this.loadRecent();
        },
        error: (err) => { this.error = err?.error?.message || 'Check-in failed'; this.cdr.markForCheck(); },
      });
  }

  checkOut(): void {
    if (!this.todayRecord) return;
    this.error = '';
    const now = new Date();
    const timeStr = now.toTimeString().split(' ')[0]; // HH:mm:ss
    const payload = {
      checkOutTime: timeStr,
      method: 'MANUAL',
      location: this.location
    };
    this.attendanceService
      .checkOut(this.todayRecord.id, payload)
      .subscribe({
        next: (r) => {
          this.todayRecord = r;
          this.success = 'Checked out successfully';
          this.cdr.markForCheck();
          this.loadMyToday();
          this.loadRecent();
        },
        error: (err) => { this.error = err?.error?.message || 'Check-out failed'; this.cdr.markForCheck(); },
      });
  }

  statusClass(s: string): string {
    return (
      {
        PRESENT: 'text-bg-success',
        LATE: 'text-bg-warning',
        ABSENT: 'text-bg-danger',
        LEAVE: 'text-bg-info',
        HALF_DAY: 'text-bg-secondary',
      }[s] || 'text-bg-light'
    );
  }
}
