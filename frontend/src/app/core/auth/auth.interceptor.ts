import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const session = authService.session();

  if (session && !authService.isSessionValid(session)) {
    authService.logout();
    void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
    return throwError(() => new Error('Session expired'));
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
        authService.logout();
        void router.navigate(['/login'], { queryParams: { returnUrl: router.url } });
      }

      return throwError(() => error);
    })
  );
};
