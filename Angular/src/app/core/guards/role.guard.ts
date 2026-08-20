import { Injectable } from '@angular/core';
import { CanActivate, CanActivateChild, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable, map } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { PermissionService } from '../services/permission.service';
import { NotificationService } from '../../shared/services/notification.service';

@Injectable({
  providedIn: 'root'
})
export class RoleGuard implements CanActivate, CanActivateChild {
  constructor(
    private authService: AuthService,
    private permissionService: PermissionService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | Observable<boolean> {
    return this.checkAccess(route);
  }

  canActivateChild(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | Observable<boolean> {
    return this.checkAccess(route);
  }

  // Checks both dimensions independently - a route can specify either, both, or
  // neither. This exists so a permission-less employee can't reach a module by typing
  // its URL directly even if it's hidden from their sidebar; the sidebar filter is a
  // UX convenience, this is the actual gate.
  private checkAccess(route: ActivatedRouteSnapshot): boolean | Observable<boolean> {
    const requiredRoles = route.data['roles'] as string[] | undefined;
    if (requiredRoles && requiredRoles.length > 0 && !this.authService.hasAnyRole(requiredRoles)) {
      return this.deny();
    }

    const requiredPermission = route.data['requiredPermission'] as string | undefined;
    if (!requiredPermission) {
      return true;
    }
    if (this.authService.isPlatformStaff()) {
      return true;
    }

    // ensureLoaded() only makes a real request the first time this page session -
    // a hard reload straight into a permission-gated route used to 403 here on a
    // stale/empty cached permission set, because nothing made this check wait for
    // the in-flight load() kicked off elsewhere (AuthService.initializeAuthState())
    // to actually land first.
    return this.permissionService.ensureLoaded().pipe(
      map(() => {
        if (!this.permissionService.hasPermission(requiredPermission)) {
          return this.deny();
        }
        return true;
      }),
    );
  }

  private deny(): boolean {
    this.notificationService.error('Insufficient permissions');
    this.router.navigate(['/forbidden']);
    return false;
  }
}
