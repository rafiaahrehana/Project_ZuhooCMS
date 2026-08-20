import { Component, OnInit, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { EmployeeService } from '../../../modules/hrm/services/employee.service';
import { EducationQualificationService } from '../../../modules/hrm/services/education-qualification.service';
import { Employee, SelfUpdateEmployeeRequest, EducationQualification, EducationQualificationRequest, GENDERS } from '../../../modules/hrm/models/hrm.model';
import { Loader } from '../loader/loader';
import { LocationComponent } from '../location/location.component';
import { FileUpload } from '../file-upload/file-upload';
import { FileUploadResult } from '../../services/file-upload.service';

@Component({
  selector: 'app-welcome',
  imports: [CommonModule, FormsModule, Loader, LocationComponent, FileUpload],
  templateUrl: './welcome.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './welcome.scss',
})
export class Welcome implements OnInit {
  @ViewChild(LocationComponent) locationComponent!: LocationComponent;

  profile: Employee | null = null;
  loading = false;
  saving = false;
  error = '';
  success = '';
  editing = false;
  genders = GENDERS;

  form: SelfUpdateEmployeeRequest = {};

  // EDUCATION QUALIFICATIONS - visible (read-only) at all times; adding/removing rows
  // only happens inside the Edit Profile form (no separate popup form).
  qualifications: EducationQualification[] = [];
  savingQualification = false;
  newQualification: Partial<EducationQualificationRequest> = {};

  constructor(
    private auth: AuthService,
    private employeeService: EmployeeService,
    private educationQualificationService: EducationQualificationService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.employeeService.getMyProfile().subscribe({
      next: (res) => {
        this.profile = res;
        this.loading = false;
        this.cdr.markForCheck();
        this.loadQualifications();
      },
      error: () => {
        this.error = 'Failed to load profile details';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  loadQualifications(): void {
    if (!this.profile) return;
    this.educationQualificationService.listForEmployee(this.profile.id).subscribe({
      next: (q) => { this.qualifications = q; this.cdr.markForCheck(); },
      error: () => { this.qualifications = []; this.cdr.markForCheck(); },
    });
  }

  addQualification(): void {
    if (!this.profile || !this.newQualification.degree || !this.newQualification.institution) return;
    this.savingQualification = true;
    const payload: EducationQualificationRequest = {
      employeeId: this.profile.id,
      degree: this.newQualification.degree,
      institution: this.newQualification.institution,
      fieldOfStudy: this.newQualification.fieldOfStudy,
      passingYear: this.newQualification.passingYear,
      result: this.newQualification.result,
      notes: this.newQualification.notes,
    };
    this.educationQualificationService.create(payload).subscribe({
      next: () => {
        this.savingQualification = false;
        this.newQualification = {};
        this.cdr.markForCheck();
        this.loadQualifications();
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

  get timeGreeting(): string {
    const hr = new Date().getHours();
    if (hr < 5) return 'Good Night';
    if (hr < 12) return 'Good Morning';
    if (hr < 17) return 'Good Afternoon';
    if (hr < 21) return 'Good Evening';
    return 'Good Night';
  }

  editPersonal(): void {
    this.form = {
      workPhone: this.profile?.workPhone || '',
      phone: this.profile?.phone || '',
      profileImageUrl: this.profile?.profileImageUrl || '',
      gender: this.profile?.gender,
      fatherName: this.profile?.fatherName || '',
      motherName: this.profile?.motherName || '',
      nationalId: this.profile?.nationalId || '',
      taxId: this.profile?.taxId || '',
      emergencyContactName: this.profile?.emergencyContactName || '',
      emergencyContactPhone: this.profile?.emergencyContactPhone || '',
      emergencyContactRelation: this.profile?.emergencyContactRelation || '',
    };
    this.editing = true;
    this.error = '';
    this.success = '';
    if (this.profile?.location) {
      setTimeout(() => {
        this.locationComponent?.resolveExistingLocation(this.profile!.location as any);
      }, 50);
    }
  }

  cancelEdit(): void {
    this.editing = false;
    this.error = '';
    this.success = '';
  }

  onAvatarUploaded(result: FileUploadResult): void {
    this.form.profileImageUrl = result.fileUrl;
  }

  save(): void {
    this.saving = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    const payload: SelfUpdateEmployeeRequest = { ...this.form };
    (Object.keys(payload) as (keyof SelfUpdateEmployeeRequest)[]).forEach((key) => {
      if (payload[key] === '' || payload[key] == null) delete payload[key];
    });

    if (this.locationComponent) {
      const formValue = this.locationComponent.locationForm.getRawValue();
      const hasAnyValue = Object.values(formValue).some((v) => v !== null && v !== '');
      if (hasAnyValue) {
        if (this.locationComponent.locationForm.invalid) {
          this.locationComponent.locationForm.markAllAsTouched();
          this.saving = false;
          this.error = 'Please fill all required address fields';
          this.cdr.markForCheck();
          return;
        }
        payload.location = formValue;
      }
    }

    this.employeeService.updateMyProfile(payload).subscribe({
      next: (res) => {
        this.profile = res;
        if (payload.profileImageUrl) {
          this.auth.updateCurrentUserImage(res.profileImageUrl);
        }
        this.success = 'Profile updated successfully';
        this.saving = false;
        this.editing = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update profile';
        this.saving = false;
        this.cdr.markForCheck();
      },
    });
  }

  getProfileImage(url: string | undefined | null): string {
    return this.auth.resolveImageUrl(url) || '';
  }

  getRoleLabel(role: string | undefined | null): string {
    if (!role) return '';
    return role
      .toLowerCase()
      .split('_')
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
      .join(' ');
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
        BD: 'Bangladesh', US: 'United States', UK: 'United Kingdom',
        GB: 'United Kingdom', IN: 'India', CA: 'Canada', AU: 'Australia',
      };
      const countryUpper = loc.country.toUpperCase();
      parts.push(countryMap[countryUpper] || loc.country);
    }
    return parts.filter((p) => !!p).join(', ');
  }

  getInitials(): string {
    const name = this.profile?.firstName || 'U';
    return name.charAt(0).toUpperCase();
  }
}
