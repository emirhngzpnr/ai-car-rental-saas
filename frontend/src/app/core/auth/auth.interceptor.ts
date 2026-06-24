import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.url.includes('/api/public/')
    || request.url.includes('/api/customer/')
    || request.url.includes('/api/auth/')) {
    return next(request);
  }

  const authService = inject(AuthService);
  const router = inject(Router);
  const session = authService.session();

  if (session && !authService.isSessionValid(session)) {
    return authService.refresh().pipe(
      switchMap((refreshedSession) => next(request.clone({
        setHeaders: {
          Authorization: `${refreshedSession.tokenType} ${refreshedSession.token}`
        }
      }))),
      catchError((error) => {
        authService.logout();
        void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
        return throwError(() => error);
      })
    );
  }

  const authorizedRequest = session?.token
    ? request.clone({
        setHeaders: {
          Authorization: `${session.tokenType} ${session.token}`
        }
      })
    : request;

  return next(authorizedRequest).pipe(
    catchError((error) => {
      if (error.status === 401) {
        return authService.refresh().pipe(
          switchMap((refreshedSession) => next(request.clone({
            setHeaders: {
              Authorization: `${refreshedSession.tokenType} ${refreshedSession.token}`
            }
          }))),
          catchError(() => {
            authService.logout();
            void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
            return throwError(() => error);
          })
        );
      }

      return throwError(() => error);
    })
  );
};
