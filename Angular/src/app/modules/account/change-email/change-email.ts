import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Change Email, split out of the Preferences page so the Authentication nav
 * group can link straight to it. Re-authenticates with the current password,
 * because the email address is the sign-in identifier.
 */
@Component({
  selector: 'app-change-email',
  imports: [CommonModule, FormsModule],
  templateUrl: './change-email.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangeEmail {
  saving = false;
  error = '';
  success = '';
  showPassword = false;

  form = { newEmail: '', currentPassword: '' };

  constructor(
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  get currentEmail(): string {
    return this.auth.getCurrentUser()?.email || '';
  }

  submit(): void {
    const newEmail = this.form.newEmail.trim();

    if (!newEmail) {
      this.error = 'New email is required';
      return;
    }
    if (!newEmail.includes('@') || newEmail.startsWith('@') || newEmail.endsWith('@')) {
      this.error = 'Enter a valid email address';
      return;
    }
    if (newEmail.toLowerCase() === this.currentEmail.toLowerCase()) {
      this.error = 'That is already your email address';
      return;
    }
    if (!this.form.currentPassword) {
      this.error = 'Enter your current password to confirm this change';
      return;
    }

    this.saving = true;
    this.error = '';
    this.success = '';
    this.cdr.markForCheck();

    this.auth.updateProfile({ email: newEmail, currentPassword: this.form.currentPassword }).subscribe({
      next: () => {
        this.saving = false;
        this.form = { newEmail: '', currentPassword: '' };
        this.success = 'Email updated successfully. Use the new address next time you sign in.';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.saving = false;
        this.error = err?.error?.message || 'Failed to update email';
        this.cdr.markForCheck();
      },
    });
  }
}
