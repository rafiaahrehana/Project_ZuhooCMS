import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router, CanActivateChild } from '@angular/router';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../../shared/services/notification.service';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate, CanActivateChild {
  constructor(
    private authService: AuthService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.checkAuth(state.url);
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean> {
    return this.checkAuth(state.url);
  }

  private checkAuth(url: string): Observable<boolean> {
    if (this.authService.isAuthenticated()) {
      return of(true);
    }

    if (this.authService.getRefreshToken()) {
      return this.authService.refreshToken().pipe(
        map(() => true),
        catchError((err) => {
          const serverRejected = err?.status === 400 || err?.status === 401 || err?.status === 403;
          if (serverRejected) {
            this.authService.clearSession();
          }
          return of(this.redirectToLogin(url, serverRejected));
        }),
      );
    }

    return of(this.redirectToLogin(url));
  }

  private redirectToLogin(url: string, skipNotification = false): boolean {
    if (url === '/' || url === '') {
      this.router.navigate(['/home']);
      return false;
    }

    if (!skipNotification) {
      this.notificationService.warning('Please log in first');
    }
    this.router.navigate(['/auth/login'], { queryParams: { returnUrl: url } });
    return false;
  }
}