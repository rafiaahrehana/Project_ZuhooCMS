import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './login.scss',
})
export class Login {
  loading = false;
  error = '';
  showPwd = false;
  form;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.cdr.markForCheck();
    this.error = '';
    this.cdr.markForCheck();
    this.authService.login(this.form.getRawValue() as any).subscribe({
      next: () => {
        const user = this.authService.getCurrentUser();
        if (user?.roles.includes('CLIENT')) {
          this.router.navigate(['/client/dashboard']);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err) => {
        if (err?.error?.message) {
          this.error = err.error.message;
        } else if (err?.message) {
          this.error = err.message;
        } else {
          this.error = 'Invalid username or password or server error';
        }
        this.cdr.markForCheck();
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
