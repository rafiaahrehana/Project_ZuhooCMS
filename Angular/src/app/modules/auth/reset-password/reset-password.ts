import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('newPassword')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword
    ? { passwordMismatch: true }
    : null;
}

// 'code' - the "forgot password" flow: ?email=... in the URL. The code is
//   verified on its own screen first (step 'code'); only once it checks out
//   does the new-password screen (step 'password') appear.
// 'token' - a client-portal invite link: ?token=... in the URL, no code step -
//   the token alone (see ResetPasswordRequest) proves the request is legitimate,
//   so this mode goes straight to step 'password'.
// 'missing' - neither query param was present.
type ResetMode = 'code' | 'token' | 'missing';
type ResetStep = 'code' | 'password';

@Component({
  selector: 'app-reset-password',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './reset-password.scss',
})
export class ResetPassword {
  loading = false;
  submitted = false;
  error = '';
  mode: ResetMode = 'missing';
  step: ResetStep = 'password';
  email = '';
  token: string | null = null;
  form;

  codeVerifyLoading = false;
  showNewPassword = false;
  showConfirmPassword = false;

  resendLoading = false;
  resendSent = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {
    this.token = this.route.snapshot.queryParamMap.get('token');
    const email = this.route.snapshot.queryParamMap.get('email');

    if (this.token) {
      this.mode = 'token';
      this.step = 'password';
    } else if (email) {
      this.mode = 'code';
      this.email = email;
      this.step = 'code';
    } else {
      this.mode = 'missing';
    }

    this.form = this.fb.group(
      {
        code: this.mode === 'code' ? ['', [Validators.required, Validators.pattern(/^\d{6}$/)]] : [''],
        newPassword: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required],
      },
      { validators: passwordsMatchValidator },
    );
  }

  // Step 1 for mode 'code': check the code on its own before showing the
  // password fields, so a mistyped code doesn't waste a password entry.
  verifyCode(): void {
    if (this.mode !== 'code' || this.codeVerifyLoading) return;
    const codeControl = this.form.controls.code;
    if (codeControl.invalid) {
      codeControl.markAsTouched();
      return;
    }
    this.codeVerifyLoading = true;
    this.error = '';
    this.cdr.markForCheck();

    this.authService.verifyResetCode({ email: this.email, code: codeControl.value! }).subscribe({
      next: () => {
        this.codeVerifyLoading = false;
        this.step = 'password';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Invalid or expired reset code.';
        this.codeVerifyLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  toggleNewPasswordVisibility(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  submit(): void {
    if (this.mode === 'missing' || this.step !== 'password' || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.cdr.markForCheck();
    this.error = '';
    this.cdr.markForCheck();

    const { code, newPassword, confirmPassword } = this.form.getRawValue();
    const payload =
      this.mode === 'token'
        ? { token: this.token, newPassword, confirmPassword }
        : { email: this.email, code, newPassword, confirmPassword };

    this.authService.resetPassword(payload as any).subscribe({
      next: () => {
        this.loading = false;
        this.submitted = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error =
          err?.error?.message ||
          (this.mode === 'token'
            ? 'This link is invalid or has expired. Please request a new one.'
            : 'Invalid or expired reset code.');
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }

  resend(): void {
    if (this.mode !== 'code' || !this.email || this.resendLoading) return;
    this.resendLoading = true;
    this.resendSent = false;
    this.cdr.markForCheck();

    this.authService.forgotPassword({ email: this.email }).subscribe({
      // Backend always returns 200 regardless of whether the email exists.
      next: () => {
        this.resendLoading = false;
        this.resendSent = true;
        this.cdr.markForCheck();
      },
      error: () => {
        this.resendLoading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
