import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

type VerifyState = 'entering' | 'success' | 'missing-email';

@Component({
  selector: 'app-verify-email',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './verify-email.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './verify-email.scss',
})
export class VerifyEmail implements OnInit {
  state: VerifyState = 'entering';
  email = '';
  loading = false;
  error = '';

  codeForm;

  resendLoading = false;
  resendSent = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
  ) {
    this.codeForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    });
  }

  ngOnInit(): void {
    const email = this.route.snapshot.queryParamMap.get('email');
    if (!email) {
      this.state = 'missing-email';
      this.cdr.markForCheck();
      return;
    }
    this.email = email;
  }

  submit(): void {
    if (this.codeForm.invalid) {
      this.codeForm.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    const { code } = this.codeForm.getRawValue();
    this.authService.verifyEmail({ email: this.email, code: code as string }).subscribe({
      next: () => {
        this.loading = false;
        this.state = 'success';
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Invalid or expired verification code.';
        this.cdr.markForCheck();
      },
    });
  }

  resend(): void {
    if (!this.email || this.resendLoading) return;
    this.resendLoading = true;
    this.resendSent = false;
    this.cdr.markForCheck();

    this.authService.resendVerification({ email: this.email }).subscribe({
      // Backend always returns 200 regardless of whether the email exists / is already verified.
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
