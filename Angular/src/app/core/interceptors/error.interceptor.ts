import { HttpErrorResponse, HttpEvent, HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { NotificationService } from '../../shared/services/notification.service';
import { SKIP_ERROR_TOAST } from './http-context-tokens';
import { extractErrorMessage } from '../utils/http-error.util';

export const errorInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn): Observable<HttpEvent<unknown>> => {
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const errorMessage = extractErrorMessage(error);

      if (error.status !== 401 && !req.context.get(SKIP_ERROR_TOAST)) {
        notificationService.error(errorMessage);
      }

      console.error('API Error:', error);
      return throwError(() => error);
    })
  );
};
