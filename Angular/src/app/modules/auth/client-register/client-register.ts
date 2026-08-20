import { Component, ChangeDetectionStrategy, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { PublicRegistrationService, PublicCompanyOption } from '../services/public-registration.service';

// Mirrors backend @Size on PublicClientRegisterRequest.password (min 8 chars,
// no complexity pattern required unlike company owner registration).
function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return password && confirmPassword && password !== confirmPassword
    ? { passwordMismatch: true }
    : null;
}

@Component({
  selector: 'app-client-register',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './client-register.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: '../register/register.scss',
})
export class ClientRegister implements OnInit {
  loading = false;
  submitted = false;
  error = '';
  showPwd = false;
  showConfirmPwd = false;
  currentStep = 1;
  form;

  companies: PublicCompanyOption[] = [];
  companiesLoading = false;
  companiesError = '';

  constructor(
    private fb: FormBuilder,
    private registrationService: PublicRegistrationService,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group(
      {
        firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
        lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
        email: ['', [Validators.required, Validators.email]],
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required],
        companyId: [null as number | null, Validators.required],
        phone: ['', Validators.maxLength(30)],
        clientCompanyName: ['', Validators.maxLength(150)],
        industry: ['', Validators.maxLength(100)],
        website: ['', Validators.maxLength(255)],
      },
      { validators: passwordsMatchValidator },
    );
  }

  ngOnInit(): void {
    this.companiesLoading = true;
    this.registrationService.getCompanies().subscribe({
      next: (companies) => {
        this.companies = companies;
        this.companiesLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.companiesError = 'Could not load the list of companies. Please try again later.';
        this.companiesLoading = false;
        this.cdr.markForCheck();
      },
    });
  }

  nextStep(): void {
    if (this.currentStep === 1) {
      const step1Controls = ['firstName', 'lastName', 'email', 'password', 'confirmPassword'];
      let valid = true;
      step1Controls.forEach(ctrl => {
        if (this.form.get(ctrl)?.invalid) {
          this.form.get(ctrl)?.markAsTouched();
          valid = false;
        }
      });
      if (this.form.hasError('passwordMismatch')) {
        valid = false;
      }
      if (valid) this.currentStep = 2;
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.error = '';
    this.cdr.markForCheck();

    const { confirmPassword, ...request } = this.form.getRawValue();

    this.registrationService.registerClient(request as any).subscribe({
      next: () => {
        this.loading = false;
        this.submitted = true;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Registration failed. Please try again.';
        this.loading = false;
        this.cdr.markForCheck();
      },
    });
  }
}
