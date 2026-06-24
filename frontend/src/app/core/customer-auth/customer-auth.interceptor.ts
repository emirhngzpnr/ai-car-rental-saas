import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { CustomerAuthService } from './customer-auth.service';

export const customerAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const isCustomerApi = request.url.includes('/api/customer/');
  const isAuthApi = request.url.includes('/api/customer/auth/');
  if (!isCustomerApi || isAuthApi) return next(request);
  const auth = inject(CustomerAuthService);
  const router = inject(Router);
  const session = auth.session();

  if (session?.token && !auth.valid(session)) {
    return auth.refresh().pipe(
      switchMap((refreshed) => next(request.clone({
        setHeaders: { Authorization: `${refreshed.tokenType} ${refreshed.token}` }
      }))),
      catchError((error) => {
        auth.logout();
        void router.navigate(['/customer/login'], { queryParams: { returnUrl: router.url } });
        return throwError(() => error);
      })
    );
  }

  const authorized = session?.token
    ? request.clone({ setHeaders: { Authorization: `${session.tokenType} ${session.token}` } }) : request;
  return next(authorized).pipe(catchError((error) => {
    if (error.status === 401) {
      return auth.refresh().pipe(
        switchMap((refreshed) => next(request.clone({
          setHeaders: { Authorization: `${refreshed.tokenType} ${refreshed.token}` }
        }))),
        catchError(() => {
          auth.logout();
          void router.navigate(['/customer/login'], { queryParams: { returnUrl: router.url } });
          return throwError(() => error);
        })
      );
    }
    return throwError(() => error);
  }));
};
