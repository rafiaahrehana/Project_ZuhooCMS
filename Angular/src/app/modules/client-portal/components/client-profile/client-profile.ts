import { Component, OnInit, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';
import { ClientService } from '../../../crm/services/client.service';
import { Client } from '../../../crm/models/crm.model';
import { Loader } from '../../../../shared/components/loader/loader';
import { FileUpload } from '../../../../shared/components/file-upload/file-upload';
import { FileUploadResult } from '../../../../shared/services/file-upload.service';
import { LocationComponent } from '../../../../shared/components/location/location.component';

@Component({
  selector: 'app-client-profile',
  imports: [CommonModule, FormsModule, Loader, FileUpload, LocationComponent],
  templateUrl: './client-profile.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientProfile implements OnInit {
  @ViewChild(LocationComponent) locationComponent!: LocationComponent;
  loading = true;
  client?: Client;
  profile: any = null;
  editingPersonal = false;

  personalForm: any = { firstName: '', lastName: '', phone: '', image: '' };
  savingPersonal = false;
  personalError = '';
  personalSuccess = '';

  editingCompany = false;
  companyForm: any = { clientCompanyName: '', industry: '', website: '', billingAddress: '', shippingAddress: '' };
  savingCompany = false;
  companyError = '';
  companySuccess = '';

  editingEmail = false;
  emailForm = { newEmail: '', currentPassword: '' };
  savingEmail = false;
  emailError = '';
  emailSuccess = '';

  editingPassword = false;
  passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
  savingPassword = false;
  passwordError = '';
  passwordSuccess = '';

  constructor(
    private auth: AuthService,
    private clientService: ClientService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.auth.getProfile().subscribe({
      next: (p: any) => {
        this.profile = p;
        this.personalForm = { firstName: p.firstName || '', lastName: p.lastName || '', phone: p.phone || '', image: p.image || '' };
        this.cdr.markForCheck();
      },
      error: () => { this.cdr.markForCheck(); },
    });
    this.clientService.getMyProfile().subscribe({
      next: (c) => {
        this.client = c;
        this.companyForm = {
          clientCompanyName: c.clientCompanyName || '',
          industry: c.industry || '',
          website: c.website || '',
          billingAddress: c.billingAddress || '',
          shippingAddress: c.shippingAddress || '',
        };
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.loading = false; this.cdr.markForCheck(); },
    });
  }

  onAvatarUploaded(result: FileUploadResult): void {
    this.personalForm.image = result.fileUrl;
  }

  editPersonal(): void {
    this.editingPersonal = true;
    this.cdr.markForCheck();
    if (this.profile && this.profile.location) {
      setTimeout(() => {
        if (this.locationComponent) {
          this.locationComponent.resolveExistingLocation(this.profile.location);
        }
      }, 50);
    }
  }

  cancelEditPersonal(): void {
    this.personalForm = {
      firstName: this.profile?.firstName || '',
      lastName: this.profile?.lastName || '',
      phone: this.profile?.phone || '',
      image: this.profile?.image || '',
    };
    this.editingPersonal = false;
    this.cdr.markForCheck();
  }

  editEmail(): void {
    this.emailForm = { newEmail: '', currentPassword: '' };
    this.emailError = '';
    this.emailSuccess = '';
    this.editingEmail = true;
    this.cdr.markForCheck();
  }

  cancelEditEmail(): void {
    this.emailForm = { newEmail: '', currentPassword: '' };
    this.editingEmail = false;
    this.cdr.markForCheck();
  }

  saveEmail(): void {
    if (!this.emailForm.newEmail.trim()) {
      this.emailError = 'New email is required';
      this.cdr.markForCheck();
      return;
    }
    if (!this.emailForm.currentPassword) {
      this.emailError = 'Enter your current password to confirm this change';
      this.cdr.markForCheck();
      return;
    }
    this.savingEmail = true;
    this.emailError = '';
    this.emailSuccess = '';
    this.cdr.markForCheck();
    this.auth.updateProfile({ email: this.emailForm.newEmail.trim(), currentPassword: this.emailForm.currentPassword }).subscribe({
      next: (res) => {
        this.profile = res;
        if (this.client) this.client.email = res.email;
        this.emailSuccess = 'Email updated';
        this.emailForm = { newEmail: '', currentPassword: '' };
        this.savingEmail = false;
        this.editingEmail = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.emailError = err?.error?.message || 'Failed to update email';
        this.savingEmail = false;
        this.cdr.markForCheck();
      },
    });
  }

  editCompany(): void {
    this.companyError = '';
    this.companySuccess = '';
    this.editingCompany = true;
    this.cdr.markForCheck();
  }

  cancelEditCompany(): void {
    this.companyForm = {
      clientCompanyName: this.client?.clientCompanyName || '',
      industry: this.client?.industry || '',
      website: this.client?.website || '',
      billingAddress: this.client?.billingAddress || '',
      shippingAddress: this.client?.shippingAddress || '',
    };
    this.editingCompany = false;
    this.cdr.markForCheck();
  }

  editPassword(): void {
    this.passwordError = '';
    this.passwordSuccess = '';
    this.editingPassword = true;
    this.cdr.markForCheck();
  }

  cancelEditPassword(): void {
    this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
    this.editingPassword = false;
    this.cdr.markForCheck();
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

  savePersonal(): void {
    this.savingPersonal = true;
    this.personalError = '';
    this.personalSuccess = '';
    this.cdr.markForCheck();

    const payload: any = { ...this.personalForm };

    if (this.locationComponent) {
      const formValue = this.locationComponent.locationForm.getRawValue();
      const hasAnyValue = Object.values(formValue).some(val => val !== null && val !== '');
      
      if (hasAnyValue) {
        if (this.locationComponent.locationForm.invalid) {
          this.locationComponent.locationForm.markAllAsTouched();
          this.savingPersonal = false;
          this.personalError = 'Please fill all required location fields';
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

    this.auth.updateProfile(payload).subscribe({
      next: (res) => {
        this.profile = res;
        this.personalSuccess = 'Profile updated';
        this.savingPersonal = false;
        this.editingPersonal = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.personalError = err?.error?.message || 'Failed to update profile';
        this.savingPersonal = false;
        this.cdr.markForCheck();
      },
    });
  }

  saveCompany(): void {
    this.savingCompany = true;
    this.companyError = '';
    this.companySuccess = '';
    this.cdr.markForCheck();
    this.clientService.updateMyProfile(this.companyForm).subscribe({
      next: (c) => {
        this.client = c;
        this.companySuccess = 'Company details updated';
        this.savingCompany = false;
        this.editingCompany = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.companyError = err?.error?.message || 'Failed to update company details';
        this.savingCompany = false;
        this.cdr.markForCheck();
      },
    });
  }

  changePassword(): void {
    if (!this.passwordForm.currentPassword || !this.passwordForm.newPassword) {
      this.passwordError = 'Current and new password are required';
      this.cdr.markForCheck();
      return;
    }
    if (this.passwordForm.newPassword !== this.passwordForm.confirmPassword) {
      this.passwordError = 'New password and confirmation do not match';
      this.cdr.markForCheck();
      return;
    }
    this.savingPassword = true;
    this.passwordError = '';
    this.passwordSuccess = '';
    this.cdr.markForCheck();
    this.auth.changePassword(this.passwordForm).subscribe({
      next: () => {
        this.passwordSuccess = 'Password changed. Please sign in again next time with your new password.';
        this.passwordForm = { currentPassword: '', newPassword: '', confirmPassword: '' };
        this.savingPassword = false;
        this.editingPassword = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.passwordError = err?.error?.message || 'Failed to change password';
        this.savingPassword = false;
        this.cdr.markForCheck();
      },
    });
  }
}
