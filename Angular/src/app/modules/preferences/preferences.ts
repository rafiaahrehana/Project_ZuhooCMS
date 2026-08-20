import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import {
  NotificationPreference,
  UpdateNotificationPreferenceRequest,
} from '../../core/models/notification.model';
import { NotificationService } from '../../core/services/notification.service';
import { Loader } from '../../shared/components/loader/loader';
import { AuthService } from '../../core/services/auth.service';

interface PrefRow {
  key: keyof UpdateNotificationPreferenceRequest;
  label: string;
  description: string;
  channel: 'Email' | 'In-app';
}

@Component({
  selector: 'app-notification-preferences',
  imports: [CommonModule, FormsModule, RouterLink, Loader],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './preferences.html',
  styleUrl: './preferences.scss'
})
export class Preferences implements OnInit {
  activeTab: 'notifications' | 'security' = 'notifications';

  // SECURITY (CHANGE PASSWORD & EMAIL) VARIABLES
  changingPassword = false;
  showCurrentPassword = false;
  showNewPassword = false;
  showConfirmPassword = false;

  editingEmail = false;
  editingPassword = false;
  changingEmail = false;

  passwordForm = {
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  emailForm = {
    newEmail: '',
    currentPassword: ''
  };

  // NOTIFICATION VARIABLES
  preference: NotificationPreference | null = null;
  loading = false;
  saving = false;
  error = '';
  success = '';

  // GROUPED ROWS FOR THE UI
  emailRows: PrefRow[] = [
    { key: 'emailOnServiceRequest', label: 'Service requests', description: 'New service requests created or received', channel: 'Email' },
    { key: 'emailOnStatusChange', label: 'Status changes', description: 'Updates on service request status transitions', channel: 'Email' },
    { key: 'emailOnInvoice', label: 'Invoices', description: 'New invoices generated for you', channel: 'Email' },
    { key: 'emailOnPayment', label: 'Payments', description: 'Payments received or processed', channel: 'Email' },
    { key: 'emailOnTaskAssigned', label: 'Task assignments', description: 'Tasks or requests assigned to you', channel: 'Email' },
    { key: 'emailOnLeaveUpdate', label: 'Leave updates', description: 'Leave request approvals or rejections', channel: 'Email' },
    { key: 'emailMarketing', label: 'Product updates', description: 'Occasional product news and announcements', channel: 'Email' },
  ];

  inAppRows: PrefRow[] = [
    { key: 'inAppOnServiceRequest', label: 'Service requests', description: 'New service requests', channel: 'In-app' },
    { key: 'inAppOnStatusChange', label: 'Status changes', description: 'Status transition updates', channel: 'In-app' },
  ];

  constructor(
    private notificationService: NotificationService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  // TAB CONTROLS
  setTab(tab: 'notifications' | 'security'): void {
    this.activeTab = tab;
    this.error = '';
    this.success = '';
  }

  // CHANGE PASSWORD
  changePassword(): void {
    const { currentPassword, newPassword, confirmPassword } = this.passwordForm;

    if (!currentPassword || !newPassword || !confirmPassword) {
      this.error = 'All password fields are required';
      return;
    }
    if (newPassword.length < 8) {
      this.error = 'New password must be at least 8 characters';
      return;
    }
    if (newPassword !== confirmPassword) {
      this.error = 'New passwords do not match';
      return;
    }

    this.changingPassword = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    this.authService.changePassword({ currentPassword, newPassword, confirmPassword }).subscribe({
      next: () => {
        this.changingPassword = false;
        this.resetPasswordForm();
        // Backend revokes ALL refresh tokens on password change, so this session
        // can no longer refresh - log out and send the user back to sign in.
        this.success = 'Password changed successfully. Redirecting to sign in...';
        this.cdr.markForCheck();
        setTimeout(() => this.authService.logout(), 2000);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to change password';
        this.changingPassword = false;
        this.cdr.markForCheck();
      }
    });
  }

  editEmail(): void {
    this.editingEmail = true;
    this.editingPassword = false;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();
  }

  cancelEmail(): void {
    this.editingEmail = false;
    this.emailForm = { newEmail: '', currentPassword: '' };
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();
  }

  saveEmail(): void {
    if (!this.emailForm.newEmail.trim()) {
      this.error = 'New email is required';
      return;
    }
    if (!this.emailForm.currentPassword) {
      this.error = 'Enter your current password to confirm this change';
      return;
    }
    this.changingEmail = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    this.authService.updateProfile({
      email: this.emailForm.newEmail.trim(),
      currentPassword: this.emailForm.currentPassword,
    }).subscribe({
      next: () => {
        this.changingEmail = false;
        this.editingEmail = false;
        this.emailForm = { newEmail: '', currentPassword: '' };
        this.success = 'Email updated successfully';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.changingEmail = false;
        this.error = err?.error?.message || 'Failed to update email';
        this.cdr.markForCheck();
      },
    });
  }

  resetPasswordForm(): void {
    this.passwordForm = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    };
    this.showCurrentPassword = false;
    this.showNewPassword = false;
    this.showConfirmPassword = false;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();
  }

  get passwordStrength(): { label: string; class: string; bars: number } {
    const pw = this.passwordForm.newPassword;
    if (!pw) return { label: '', class: '', bars: 0 };
    let score = 0;
    if (pw.length >= 8) score++;
    if (/[A-Z]/.test(pw)) score++;
    if (/[a-z]/.test(pw)) score++;
    if (/[0-9]/.test(pw)) score++;
    if (/[!@#$%^&*()_+\-=\[\]{};':\\|,.<>\/?]/.test(pw)) score++;

    if (score <= 2) return { label: 'Weak', class: 'text-danger', bars: 1 };
    if (score <= 4) return { label: 'Medium', class: 'text-warning', bars: 3 };
    return { label: 'Strong', class: 'text-success', bars: 5 };
  }

  // LOAD PREFERENCES
  load(): void {
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();
    this.notificationService.getPreferences().subscribe({
      next: (res: NotificationPreference) => { this.preference = res; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.error = 'Failed to load preferences'; this.loading = false; this.cdr.markForCheck(); }
    });
  }

  // SAVE PREFERENCES
  save(): void {
    if (!this.preference) return;
    this.saving = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();
    const payload: UpdateNotificationPreferenceRequest = {
      emailOnServiceRequest: this.preference.emailOnServiceRequest,
      emailOnStatusChange: this.preference.emailOnStatusChange,
      emailOnInvoice: this.preference.emailOnInvoice,
      emailOnPayment: this.preference.emailOnPayment,
      emailOnTaskAssigned: this.preference.emailOnTaskAssigned,
      emailOnLeaveUpdate: this.preference.emailOnLeaveUpdate,
      inAppOnServiceRequest: this.preference.inAppOnServiceRequest,
      inAppOnStatusChange: this.preference.inAppOnStatusChange,
      emailMarketing: this.preference.emailMarketing,
    };
    this.notificationService.updatePreferences(payload).subscribe({
      next: (res: NotificationPreference) => { this.preference = res; this.saving = false; this.success = 'Preferences saved'; this.cdr.markForCheck(); },
      error: (err: any) => { this.saving = false; this.error = err?.error?.message || 'Failed to save preferences'; this.cdr.markForCheck(); }
    });
  }

  togglePreference(key: keyof UpdateNotificationPreferenceRequest, val: boolean): void {
    if (!this.preference) return;
    this.preference[key] = val;

    const payload: UpdateNotificationPreferenceRequest = {
      emailOnServiceRequest: this.preference.emailOnServiceRequest,
      emailOnStatusChange: this.preference.emailOnStatusChange,
      emailOnInvoice: this.preference.emailOnInvoice,
      emailOnPayment: this.preference.emailOnPayment,
      emailOnTaskAssigned: this.preference.emailOnTaskAssigned,
      emailOnLeaveUpdate: this.preference.emailOnLeaveUpdate,
      inAppOnServiceRequest: this.preference.inAppOnServiceRequest,
      inAppOnStatusChange: this.preference.inAppOnStatusChange,
      emailMarketing: this.preference.emailMarketing,
    };

    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    this.notificationService.updatePreferences(payload).subscribe({
      next: (res: NotificationPreference) => {
        this.preference = res;
        this.success = 'Notification preference updated';
        this.cdr.markForCheck();
        setTimeout(() => {
          if (this.success === 'Notification preference updated') {
            this.success = '';
            this.cdr.markForCheck();
          }
        }, 3000);
      },
      error: (err: any) => {
        this.error = err?.error?.message || 'Failed to save preferences';
        this.cdr.markForCheck();
      }
    });
  }

  // RESET TO DEFAULTS
  reset(): void {
    this.notificationService.resetPreferences().subscribe({
      next: (res: NotificationPreference) => { this.preference = res; this.success = 'Preferences reset to defaults'; this.cdr.markForCheck(); },
      error: (err: any) => { this.error = err?.error?.message || 'Failed to reset preferences'; this.cdr.markForCheck(); }
    });
  }
}
