import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class ClientHomeRedirectGuard implements CanActivate {
  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  canActivate(): boolean {
    if (this.authService.hasRole('CLIENT')) {
      this.router.navigate(['/client/dashboard']);
      return false;
    }
    return true;
  }
}