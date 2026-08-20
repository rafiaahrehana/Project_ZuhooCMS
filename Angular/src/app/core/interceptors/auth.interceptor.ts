import { HttpEvent, HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { BehaviorSubject, Subject, Observable, race, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);
const refreshFailed$ = new Subject<void>();

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
  const authService = inject(AuthService);

  if (isAuthEndpoint(req.url)) {
    return next(req);
  }

  const token = authService.getAccessToken();
  if (token) {
    req = addToken(req, token);
  }

  return next(req).pipe(
    catchError(error => {
      if (error.status === 401) {
        return handle401(req, next, authService);
      }
      return throwError(() => error);
    })
  );
};

function handle401(req: HttpRequest<unknown>, next: HttpHandlerFn, authService: AuthService): Observable<HttpEvent<unknown>> {
  if (!authService.getRefreshToken()) {
    authService.logout();
    return throwError(() => new Error('Session expired. Please log in again.'));
  }

  if (isRefreshing) {
    return race(
      refreshedToken$.pipe(
        filter((token): token is string => token !== null),
        take(1),
      ),
      refreshFailed$.pipe(
        take(1),
        switchMap(() => throwError(() => new Error('Session refresh failed'))),
      ),
    ).pipe(switchMap(token => next(addToken(req, token as string))));
  }

  isRefreshing = true;
  refreshedToken$.next(null);

  return authService.refreshToken().pipe(
    switchMap(response => {
      isRefreshing = false;
      refreshedToken$.next(response.accessToken);
      return next(addToken(req, response.accessToken));
    }),
    catchError(err => {
      isRefreshing = false;
      refreshFailed$.next();
      if (err.status === 400 || err.status === 401 || err.status === 403) {
        authService.logout();
      }
      return throwError(() => err);
    })
  );
}

function addToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });
}

function isAuthEndpoint(url: string): boolean {
  return url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/refresh');
}
