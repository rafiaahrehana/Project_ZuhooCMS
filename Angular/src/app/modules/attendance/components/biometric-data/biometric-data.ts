import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  BiometricDevice, BiometricDeviceRequest, BIOMETRIC_DEVICE_TYPES,
  BiometricEnrollment, BiometricEnrollmentRequest,
} from '../../models/attendance.model';
import { BiometricDeviceService } from '../../services/biometric-device.service';
import { BiometricDataService } from '../../services/biometric-data.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

type Tab = 'devices' | 'enrollments';

@Component({
  selector: 'app-biometric-data-page',
  imports: [CommonModule, FormsModule, Pagination, Loader, EmptyState, ConfirmDialog, HasPermissionDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './biometric-data.html',
})
export class BiometricDataPage implements OnInit {
  tab: Tab = 'devices';
  error = '';
  success = '';

  // DEVICES
  devices: BiometricDevice[] = [];
  devicePage = 0;
  deviceTotalPages = 0;
  deviceLoading = false;
  showDeviceForm = false;
  editingDeviceId: number | null = null;
  deviceForm: BiometricDeviceRequest = this.emptyDeviceForm();
  deviceTypes = BIOMETRIC_DEVICE_TYPES;
  deleteDeviceTarget: BiometricDevice | null = null;

  // ENROLLMENTS (looked up by employee, since there's no "list all" endpoint)
  employeeIdLookup?: number;
  enrollments: BiometricEnrollment[] = [];
  enrollmentLoading = false;
  showEnrollForm = false;
  enrollForm: BiometricEnrollmentRequest = this.emptyEnrollForm();
  deleteEnrollmentTarget: BiometricEnrollment | null = null;

  constructor(private deviceService: BiometricDeviceService, private dataService: BiometricDataService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadDevices();
  }

  emptyDeviceForm(): BiometricDeviceRequest {
    return { deviceName: '', deviceType: 'FINGERPRINT_TERMINAL', deviceId: '', portNumber: 0, matchThreshold: 95, enabledForCheckIn: true, enabledForCheckOut: true };
  }

  emptyEnrollForm(): BiometricEnrollmentRequest {
    return { employeeId: this.employeeIdLookup || 0, deviceId: 0, biometricType: 'FINGERPRINT', biometricTemplate: '', qualityScore: 100 };
  }

  // DEVICES
  loadDevices(): void {
    this.deviceLoading = true;
    this.cdr.markForCheck();
    this.deviceService.list(this.devicePage).subscribe({
      next: (res) => {
        this.devices = res.content;
        this.deviceTotalPages = res.totalPages;
        this.deviceLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load devices';
        this.deviceLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openCreateDevice(): void {
    this.editingDeviceId = null;
    this.deviceForm = this.emptyDeviceForm();
    this.showDeviceForm = true;
  }

  openEditDevice(d: BiometricDevice): void {
    this.editingDeviceId = d.id;
    this.deviceForm = {
      deviceName: d.deviceName,
      deviceType: d.deviceType,
      deviceId: d.deviceId,
      ipAddress: d.ipAddress,
      portNumber: d.portNumber,
      location: d.location,
      department: d.department,
      matchThreshold: d.matchThreshold,
      enabledForCheckIn: d.enabledForCheckIn,
      enabledForCheckOut: d.enabledForCheckOut,
      notes: d.notes,
    };
    this.showDeviceForm = true;
  }

  saveDevice(): void {
    const op = this.editingDeviceId
      ? this.deviceService.update(this.editingDeviceId, this.deviceForm)
      : this.deviceService.create(this.deviceForm);
    op.subscribe({
      next: () => {
        this.showDeviceForm = false;
        this.success = this.editingDeviceId ? 'Device updated' : 'Device registered';
        this.cdr.markForCheck();
        this.loadDevices();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to save device'; this.cdr.markForCheck(); },
    });
  }

  doDeleteDevice(): void {
    if (!this.deleteDeviceTarget) return;
    this.deviceService.delete(this.deleteDeviceTarget.id).subscribe({
      next: () => {
        this.deleteDeviceTarget = null;
        this.success = 'Device deleted';
        this.cdr.markForCheck();
        this.loadDevices();
      },
      error: () => {
        this.deleteDeviceTarget = null;
        this.error = 'Cannot delete device';
        this.cdr.markForCheck();
      },
    });
  }

  goToDevicePage(p: number): void {
    this.devicePage = p;
    this.loadDevices();
  }

  // ENROLLMENTS
  loadEnrollments(): void {
    if (!this.employeeIdLookup) return;
    this.enrollmentLoading = true;
    this.cdr.markForCheck();
    this.dataService.getByEmployee(this.employeeIdLookup).subscribe({
      next: (res) => {
        this.enrollments = res;
        this.enrollmentLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load enrollments for that employee';
        this.enrollmentLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  openEnroll(): void {
    this.enrollForm = this.emptyEnrollForm();
    this.showEnrollForm = true;
  }

  saveEnroll(): void {
    this.dataService.enroll(this.enrollForm).subscribe({
      next: () => {
        this.showEnrollForm = false;
        this.success = 'Employee enrolled';
        this.cdr.markForCheck();
        this.loadEnrollments();
      },
      error: (err) => { this.error = err?.error?.message || 'Failed to enroll'; this.cdr.markForCheck(); },
    });
  }

  doDeleteEnrollment(): void {
    if (!this.deleteEnrollmentTarget) return;
    this.dataService.delete(this.deleteEnrollmentTarget.id).subscribe({
      next: () => {
        this.deleteEnrollmentTarget = null;
        this.success = 'Enrollment removed';
        this.cdr.markForCheck();
        this.loadEnrollments();
      },
      error: () => {
        this.deleteEnrollmentTarget = null;
        this.error = 'Cannot delete enrollment';
        this.cdr.markForCheck();
      },
    });
  }

  statusClass(status: string): string {
    return (
      { ACTIVE: 'text-bg-success', INACTIVE: 'text-bg-secondary', MAINTENANCE: 'text-bg-warning', OFFLINE: 'text-bg-danger' }[status] ||
      'text-bg-secondary'
    );
  }
}
