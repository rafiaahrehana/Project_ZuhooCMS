import { Component, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LocationComponent } from '../../../shared/components/location/location.component';

// Mirrors backend @Pattern on RegisterRequest.password:
// min 8 chars, at least one lowercase, one uppercase, one digit, one special character.
const PASSWORD_PATTERN =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+\-=]).{8,}$/;

// Mirrors backend @Pattern on RegisterRequest.subdomain.
const SUBDOMAIN_PATTERN = /^[a-z0-9]([a-z0-9-]{1,48}[a-z0-9])?$/;

function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword
    ? { passwordMismatch: true }
    : null;
}

@Component({
  selector: 'app-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LocationComponent],
  templateUrl: './register.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './register.scss',
})
export class Register {
  loading = false;
  error = '';
  showPwd = false;
  showConfirmPwd = false;
  currentStep = 1;
  form;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group(
      {
        firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
        lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
        confirmPassword: ['', Validators.required],
        companyName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(150)]],
        subdomain: ['', [Validators.required, Validators.pattern(SUBDOMAIN_PATTERN)]],
        companyEmail: ['', [Validators.email]],
        companyPhone: ['', Validators.maxLength(30)],
        location: [null as any],
      },
      { validators: passwordsMatchValidator },
    );
  }

  // Convenience helper so the subdomain preview updates as the company name is typed,
  // without overriding anything the user has already typed into the subdomain field.
  onCompanyNameInput(): void {
    const subdomainControl = this.form.controls.subdomain;
    if (subdomainControl.pristine) {
      const slug = (this.form.controls.companyName.value || '')
        .toLowerCase()
        .trim()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '')
        .slice(0, 50);
      subdomainControl.setValue(slug);
      subdomainControl.markAsPristine();
    }
  }

  onLocationChange(loc: any): void {
    this.form.controls.location.setValue(loc);
  }

  nextStep(): void {
    if (this.currentStep === 1) {
      // Validate step 1 fields
      const step1Controls = ['firstName', 'lastName', 'email', 'password', 'confirmPassword'];
      let valid = true;
      step1Controls.forEach(ctrl => {
        if (this.form.get(ctrl)?.invalid) {
          this.form.get(ctrl)?.markAsTouched();
          valid = false;
        }
      });
      // also check password mismatch
      if (this.form.hasError('passwordMismatch')) {
        valid = false;
      }
      if (valid) this.currentStep = 2;
    } else if (this.currentStep === 2) {
      // Validate step 2 fields
      const step2Controls = ['companyName', 'subdomain', 'companyEmail', 'companyPhone'];
      let valid = true;
      step2Controls.forEach(ctrl => {
        if (this.form.get(ctrl)?.invalid) {
          this.form.get(ctrl)?.markAsTouched();
          valid = false;
        }
      });
      if (valid) this.currentStep = 3;
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  skipLocationAndSubmit(): void {
    this.form.controls.location.setValue(null);
    this.submit();
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

    const { confirmPassword, ...request } = this.form.getRawValue();

    this.authService.register(request as any).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.markForCheck();
        this.router.navigate(['/auth/verify-email'], { queryParams: { email: request.email } });
      },
      error: (err) => {
        this.error = err?.error?.message || 'Registration failed. Please try again.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
