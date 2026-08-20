import { Component, OnInit, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  Employee,
  UpdateEmployeeRequest,
  Department,
  Designation,
  EducationQualification,
  EducationQualificationRequest,
  EMPLOYMENT_STATUSES,
  EMPLOYMENT_TYPES,
  GENDERS,
} from '../../models/hrm.model';
import { EmployeeService } from '../../services/employee.service';
import { DepartmentService } from '../../services/department.service';
import { DesignationService } from '../../services/designation.service';
import { EducationQualificationService } from '../../services/education-qualification.service';
import { Loader } from '../../../../shared/components/loader/loader';
import { LocationComponent } from '../../../../shared/components/location/location.component';
import { CustomRoleService } from '../../../roles-permissions/services/custom-role.service';
import { CustomRole } from '../../../roles-permissions/models/roles-permissions.model';
import { HasPermissionDirective } from '../../../../shared/directives/has-permission.directive';

@Component({
  selector: 'app-employee-detail',
  imports: [CommonModule, FormsModule, RouterLink, Loader, LocationComponent, HasPermissionDirective],
  templateUrl: './employee-detail.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './employee-detail.scss',
})
export class EmployeeDetail implements OnInit {
  @ViewChild(LocationComponent) locationComponent!: LocationComponent;
  employee: Employee | null = null;
  departments: Department[] = [];
  designations: Designation[] = [];
  loading = false;
  saving = false;
  editing = false;
  error = '';
  success = '';
  form: UpdateEmployeeRequest = {};

  statuses = EMPLOYMENT_STATUSES;
  types = EMPLOYMENT_TYPES;
  genders = GENDERS;

  customRoles: CustomRole[] = [];
  selectedRoleId: number | null = null;
  savingRole = false;

  qualifications: EducationQualification[] = [];
  addingQualification = false;
  savingQualification = false;
  qualificationForm: Partial<EducationQualificationRequest> = {};

  constructor(
    private route: ActivatedRoute,
    private employeeService: EmployeeService,
    private departmentService: DepartmentService,
    private designationService: DesignationService,
    private customRoleService: CustomRoleService,
    private educationQualificationService: EducationQualificationService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.load(id);
    this.departmentService.listActive().subscribe({ next: (d) => { this.departments = d; this.cdr.markForCheck(); } });
    this.designationService.listActive().subscribe({ next: (d) => { this.designations = d; this.cdr.markForCheck(); } });
    this.customRoleService.list().subscribe({ next: (r) => { this.customRoles = r; this.cdr.markForCheck(); } });
    this.loadQualifications(id);
  }

  loadQualifications(employeeId: number): void {
    this.educationQualificationService.listForEmployee(employeeId).subscribe({
      next: (q) => { this.qualifications = q; this.cdr.markForCheck(); },
      error: () => { this.qualifications = []; this.cdr.markForCheck(); },
    });
  }

  startAddQualification(): void {
    this.qualificationForm = {};
    this.addingQualification = true;
  }

  cancelAddQualification(): void {
    this.addingQualification = false;
    this.qualificationForm = {};
  }

  saveQualification(): void {
    if (!this.employee || !this.qualificationForm.degree || !this.qualificationForm.institution) return;
    this.savingQualification = true;
    const payload: EducationQualificationRequest = {
      employeeId: this.employee.id,
      degree: this.qualificationForm.degree,
      institution: this.qualificationForm.institution,
      fieldOfStudy: this.qualificationForm.fieldOfStudy,
      passingYear: this.qualificationForm.passingYear,
      result: this.qualificationForm.result,
      notes: this.qualificationForm.notes,
    };
    this.educationQualificationService.create(payload).subscribe({
      next: () => {
        this.savingQualification = false;
        this.addingQualification = false;
        this.qualificationForm = {};
        this.cdr.markForCheck();
        this.loadQualifications(this.employee!.id);
      },
      error: (err) => {
        this.savingQualification = false;
        this.error = err?.error?.message || 'Failed to add qualification';
        this.cdr.markForCheck();
      },
    });
  }

  deleteQualification(q: EducationQualification): void {
    this.educationQualificationService.delete(q.id).subscribe({
      next: () => { this.qualifications = this.qualifications.filter((x) => x.id !== q.id); this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to delete qualification'; this.cdr.markForCheck(); },
    });
  }

  load(id: number): void {
    this.loading = true;
    this.employeeService.getById(id).subscribe({
      next: (emp) => {
        this.employee = emp;
        this.selectedRoleId = emp.customRoleId ?? null;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'Failed to load employee';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  startEdit(): void {
    if (!this.employee) return;
    const e = this.employee;
    this.form = {
      jobTitle: e.jobTitle,
      designationId: e.designationId,
      employmentType: e.employmentType,
      employmentStatus: e.employmentStatus,
      gender: e.gender,
      dateOfBirth: e.dateOfBirth,
      hireDate: e.hireDate,
      departmentId: e.departmentId,
      basicSalary: e.basicSalary,
      houseRent: e.houseRent,
      medicalAllowance: e.medicalAllowance,
      transportAllowance: e.transportAllowance,
      billableRate: e.billableRate,
      bankName: e.bankName,
      officialEmail: e.officialEmail,
      workPhone: e.workPhone,
      officeLocation: e.officeLocation,
      costCenter: e.costCenter,
      nationalId: e.nationalId,
      taxId: e.taxId,
      emergencyContactName: e.emergencyContactName,
      emergencyContactPhone: e.emergencyContactPhone,
      emergencyContactRelation: e.emergencyContactRelation,
    };
    if (e.location) {
      this.form.location = { ...e.location } as any;
      setTimeout(() => {
        if (this.locationComponent) {
          this.locationComponent.resolveExistingLocation(e.location as any);
        }
      }, 50);
    }
    this.editing = true;
    this.success = '';
  }

  getFormattedAddress(loc: any): string {
    if (!loc) return 'Not Provided';
    const parts: string[] = [];
    if (loc.apartment) parts.push(loc.apartment);
    if (loc.streetAddress) parts.push(loc.streetAddress);
    if (loc.postalCode) parts.push(loc.postalCode);
    if (loc.level4) parts.push(loc.level4);
    if (loc.level3) parts.push(loc.level3);
    if (loc.level2) parts.push(loc.level2);
    if (loc.level1) parts.push(loc.level1);
    if (loc.country) {
      const countryMap: { [key: string]: string } = {
        'BD': 'Bangladesh',
        'US': 'United States',
        'UK': 'United Kingdom',
        'GB': 'United Kingdom',
        'IN': 'India',
        'CA': 'Canada',
        'AU': 'Australia'
      };
      const countryUpper = loc.country.toUpperCase();
      parts.push(countryMap[countryUpper] || loc.country);
    }
    return parts.filter(p => !!p).join(', ');
  }

  save(): void {
    if (!this.employee) return;
    this.saving = true;
    this.error = '';
    const payload: any = { ...this.form };

    if (this.locationComponent) {
      const formValue = this.locationComponent.locationForm.getRawValue();
      const hasAnyValue = Object.values(formValue).some(val => val !== null && val !== '');
      
      if (hasAnyValue) {
        if (this.locationComponent.locationForm.invalid) {
          this.locationComponent.locationForm.markAllAsTouched();
          this.saving = false;
          this.error = 'Please fill all required location fields';
          this.cdr.markForCheck();
          return;
        }
        payload.location = formValue;
        // Trim strings
        Object.keys(payload.location).forEach(key => {
          const val = payload.location[key];
          if (typeof val === 'string') {
            payload.location[key] = val.trim();
          }
        });
      } else {
        payload.location = null;
      }
    }

    Object.keys(payload).forEach((k) => {
      if (payload[k] === '' || payload[k] === null) delete payload[k];
    });
    this.employeeService.update(this.employee.id, payload).subscribe({
      next: (emp) => {
        this.employee = emp;
        this.saving = false;
        this.editing = false;
        this.success = 'Employee updated successfully';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to update employee';
        this.cdr.markForCheck();
      },
    });
  }

  changeRole(): void {
    if (!this.employee) return;
    this.savingRole = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    const done = (roleId: number | null, message: string) => {
      this.employee = {
        ...this.employee!,
        customRoleId: roleId ?? undefined,
        customRoleName: roleId ? this.customRoles.find((r) => r.id === roleId)?.name : undefined,
      };
      this.savingRole = false;
      this.success = message;
      this.cdr.markForCheck();
    };

    if (this.selectedRoleId) {
      this.customRoleService.assignEmployee(this.selectedRoleId, this.employee.id).subscribe({
        next: () => done(this.selectedRoleId, 'Role updated'),
        error: (err) => {
          this.savingRole = false;
          this.error = err?.error?.message || 'Failed to assign role';
          this.cdr.markForCheck();
        },
      });
    } else {
      this.customRoleService.unassignEmployee(this.employee.id).subscribe({
        next: () => done(null, 'Role removed'),
        error: (err) => {
          this.savingRole = false;
          this.error = err?.error?.message || 'Failed to remove role';
          this.cdr.markForCheck();
        },
      });
    }
  }
}
