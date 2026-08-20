import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * The company dashboard (data cards, charts) is built around permission-gated
 * admin/manager sections - a plain EMPLOYEE role is meant to land on their own
 * Employee Dashboard (profile, clock in/out, leave/payslip shortcuts) instead,
 * regardless of which permissions they've been granted. Everyone else (platform
 * staff, CLIENT) already has its own landing flow and is untouched here.
 */
@Injectable({ providedIn: 'root' })
export class DashboardAccessGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  canActivate(): boolean {
    const roles = this.authService.getCurrentUser()?.roles ?? [];
    const isRestrictedEmployee = roles.includes('EMPLOYEE') && !roles.includes('COMPANY_OWNER');

    if (isRestrictedEmployee) {
      this.router.navigate(['/employee-dashboard']);
      return false;
    }
    return true;
  }
}
