import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  Employee,
  CreateEmployeeRequest,
  Department,
  Designation,
  EmploymentStatus,
  EMPLOYMENT_STATUSES,
  EMPLOYMENT_TYPES,
  EducationQualificationRequest
} from '../../models/hrm.model';
import { EmployeeService } from '../../services/employee.service';
import { EducationQualificationService } from '../../services/education-qualification.service';
import { DepartmentService } from '../../services/department.service';
import { DesignationService } from '../../services/designation.service';
import { ShiftService } from '../../services/shift.service';
import { Pagination } from '../../../../shared/components/pagination/pagination';
import { Loader } from '../../../../shared/components/loader/loader';
import { EmptyState } from '../../../../shared/components/empty-state/empty-state';
import { ConfirmDialog } from '../../../../shared/components/confirm-dialog/confirm-dialog';
import { LocationComponent } from '../../../../shared/components/location/location.component';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { FileUploadResult } from '../../../../shared/services/file-upload.service';
import { CustomRoleService } from '../../../roles-permissions/services/custom-role.service';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';
import { CustomRole } from '../../../roles-permissions/models/roles-permissions.model';

import { BosCurrencyPipe } from '../../../../shared/pipes/bos-currency.pipe';
@Component({
  selector: 'app-employees',
  imports: [BosCurrencyPipe, CommonModule, FormsModule, RouterLink, Pagination, Loader, EmptyState, ConfirmDialog, LocationComponent, FileUpload, HasPermissionDirective],
  templateUrl: './employees.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './employees.scss',
})
export class Employees implements OnInit {
  @ViewChild(LocationComponent) locationComponent!: LocationComponent;

  employees: Employee[] = [];
  departments: Department[] = [];
  designations: Designation[] = [];
  shifts: any[] = [];
  totalPages = 0;
  page = 0;
  loading = false;
  error = '';
  success = '';
  departmentFilter: number | '' = '';
  statusFilter: EmploymentStatus | '' = '';
  searchQuery = '';

  statuses = EMPLOYMENT_STATUSES;
  types = EMPLOYMENT_TYPES;

  showForm = false;
  saving = false;
  form: CreateEmployeeRequest = this.emptyForm();
  confirmPassword = '';
  showPassword = false;
  showConfirmPassword = false;
  customRoles: CustomRole[] = [];
  assignRoleId: number | null = null;
  qualifications: Partial<EducationQualificationRequest>[] = [
    { degree: '', institution: '', fieldOfStudy: '', passingYear: undefined, result: '' }
  ];

  terminateTarget: Employee | null = null;

  constructor(
    private employeeService: EmployeeService,
    private departmentService: DepartmentService,
    private designationService: DesignationService,
    private shiftService: ShiftService,
    private customRoleService: CustomRoleService,
    private educationService: EducationQualificationService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
    this.departmentService.listActive().subscribe({ next: (d) => { this.departments = d; this.cdr.markForCheck(); } });
    this.designationService.listActive().subscribe({ next: (d) => { this.designations = d; this.cdr.markForCheck(); } });
    this.shiftService.listActive().subscribe({ next: (res) => { this.shifts = res; this.cdr.markForCheck(); } });
    this.customRoleService.list().subscribe({ next: (r) => { this.customRoles = r; this.cdr.markForCheck(); } });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.employeeService
      .list(
        this.page,
        20,
        this.departmentFilter || undefined,
        this.statusFilter || undefined,
        true,
        this.searchQuery || undefined
      )
      .subscribe({
        next: (res) => {
          this.employees = res.content;
          this.totalPages = res.totalPages;
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'Failed to load employees';
          this.loading = false;
          this.cdr.markForCheck();
        },
      });
  }

  onSearch(): void {
    this.page = 0;
    this.load();
  }

  onFilterChange(): void {
    this.page = 0;
    this.load();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.departmentFilter = '';
    this.statusFilter = '';
    this.page = 0;
    this.load();
  }

  /**
   * True when a routing number has been typed but isn't exactly 9 digits.
   * Blank is valid - the field is optional; it only has to be well-formed if
   * present, because the bank rejects a malformed one and the salary bounces.
   *
   * Format only. The 9th digit is a check digit, but the authoritative
   * algorithm isn't implemented: guessing it would reject valid numbers.
   */
  get routingInvalid(): boolean {
    const v = (this.form.bankRoutingNumber ?? '').trim();
    return v.length > 0 && !/^[0-9]{9}$/.test(v);
  }

  save(): void {
    if (this.routingInvalid) {
      this.error = 'Routing number must be exactly 9 digits';
      this.cdr.markForCheck();
      return;
    }

    if (this.locationComponent && this.locationComponent.locationForm) {
      const locFormValue = this.locationComponent.locationForm.getRawValue();
      const hasAnyLocValue = Object.values(locFormValue).some(val => val !== null && val !== '');
      if (hasAnyLocValue) {
        if (this.locationComponent.locationForm.invalid) {
          this.locationComponent.locationForm.markAllAsTouched();
          this.error = 'Please fill all required location fields';
          this.cdr.markForCheck();
          return;
        }
        this.form.location = locFormValue as any;
        Object.keys(this.form.location!).forEach(k => {
          if (typeof (this.form.location as any)[k] === 'string') {
            (this.form.location as any)[k] = (this.form.location as any)[k].trim();
          }
        });
      } else {
        delete this.form.location;
      }
    }

    this.saving = true;
    this.error = '';
    this.employeeService.create(this.cleanPayload()).subscribe({
      next: (created) => {
        const roleId = this.assignRoleId;
        const qualsToCreate = this.qualifications.filter(q => q.degree && q.institution);
        
        const finish = (message: string) => {
          this.saving = false;
          this.showForm = false;
          this.form = this.emptyForm();
          this.assignRoleId = null;
          this.success = message;
          this.page = 0;
          this.cdr.markForCheck();
          this.load();
        };

        const handleQualsAndFinish = (baseMessage: string) => {
          if (qualsToCreate.length === 0) {
            finish(baseMessage);
            return;
          }
          
          let completed = 0;
          let hasError = false;
          qualsToCreate.forEach(q => {
            const payload = { ...q, employeeId: created.id } as EducationQualificationRequest;
            this.educationService.create(payload).subscribe({
              next: () => {
                completed++;
                if (completed === qualsToCreate.length) finish(baseMessage + (hasError ? ' (some qualifications failed)' : ''));
              },
              error: () => {
                hasError = true;
                completed++;
                if (completed === qualsToCreate.length) finish(baseMessage + ' (some qualifications failed)');
              }
            });
          });
        };

        if (roleId) {
          // Role assignment needs the employee's own id, so it's a follow-up
          // call after creation rather than part of CreateEmployeeRequest.
          this.customRoleService.assignEmployee(roleId, created.id).subscribe({
            next: () => handleQualsAndFinish('Employee created and role assigned'),
            error: () => handleQualsAndFinish('Employee created, but assigning the role failed'),
          });
        } else {
          handleQualsAndFinish('Employee created successfully');
        }
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to create employee';
        this.cdr.markForCheck();
      },
    });
  }

  confirmTerminate(): void {
    if (!this.terminateTarget) return;
    this.employeeService.terminate(this.terminateTarget.id).subscribe({
      next: () => {
        this.terminateTarget = null;
        this.success = 'Employee terminated';
        this.cdr.markForCheck();
        this.load();
      },
      error: () => {
        this.terminateTarget = null;
        this.error = 'Failed to terminate employee';
        this.cdr.markForCheck();
      },
    });
  }

  goToPage(p: number): void {
    this.page = p;
    this.load();
  }

  downloadPdf(): void {
    this.employeeService
      .downloadPdf(this.departmentFilter || undefined, this.statusFilter || undefined, this.searchQuery || undefined, true)
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'employees.pdf';
          a.click();
          URL.revokeObjectURL(url);
        },
        error: () => { this.error = 'Failed to download employees PDF'; this.cdr.markForCheck(); },
      });
  }

  private cleanPayload(): CreateEmployeeRequest {
    const payload: any = { ...this.form };
    Object.keys(payload).forEach((k) => {
      if (payload[k] === '' || payload[k] === null) delete payload[k];
    });
    return payload;
  }

  addQualification(): void {
    this.qualifications.push({ degree: '', institution: '', fieldOfStudy: '', passingYear: undefined, result: '' });
  }

  removeQualification(index: number): void {
    this.qualifications.splice(index, 1);
  }

  private emptyForm(): CreateEmployeeRequest {
    this.confirmPassword = '';
    this.showPassword = false;
    this.showConfirmPassword = false;
    this.qualifications = [
      { degree: '', institution: '', fieldOfStudy: '', passingYear: undefined, result: '' }
    ];
    return {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      employmentType: 'FULL_TIME',
    };
  }

  onLocationChange(location: any) {
    this.form.location = location;
  }

  onAvatarUploaded(result: FileUploadResult): void {
    this.form.profileImageUrl = result.fileUrl;
  }

  /**
   * Accent for a card, rotating through the app's shared palette.
   *
   * Keyed on the employee id rather than the row index so a given person keeps
   * the same colour as you page through the list or filter it - an accent that
   * reshuffles on every render reads as noise rather than identity.
   */
  private static readonly CARD_ACCENTS = [
    '#0D9488', '#F59E0B', '#10B981', '#6366F1',
    '#8B5CF6', '#2563EB', '#E11D48', '#65A30D',
  ];

  accentFor(emp: Employee): string {
    const key = emp.id ?? 0;
    return Employees.CARD_ACCENTS[key % Employees.CARD_ACCENTS.length];
  }

  /** Initials for the avatar fallback when there is no photo. */
  initials(emp: Employee): string {
    return ((emp.firstName || '').charAt(0) + (emp.lastName || '').charAt(0)).toUpperCase() || '?';
  }

  /**
   * Status pill styling. Only the states that carry a warning are coloured -
   * if every status is loud, none of them is.
   */
  statusPill(status: string | undefined): string {
    switch (status) {
      case 'ACTIVE':
      case 'CONFIRMED':
        return 'pill-active';
      case 'PROBATION':
        return 'pill-warn';
      case 'ON_LEAVE':
        return 'pill-info';
      case 'SUSPENDED':
      case 'TERMINATED':
      case 'RESIGNED':
      case 'RETIRED':
        return 'pill-danger';
      default:
        return 'pill-muted';
    }
  }

  statusLabel(status: string | undefined): string {
    return (status || 'UNKNOWN').replace(/_/g, ' ');
  }
}
