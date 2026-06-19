import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { CustomerAuthService } from './customer-auth.service';

export const customerAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const isCustomerApi = request.url.includes('/api/customer/');
  const isAuthApi = request.url.includes('/api/customer/auth/');
  if (!isCustomerApi || isAuthApi) return next(request);
  const auth = inject(CustomerAuthService);
  const router = inject(Router);
  const session = auth.session();
  const authorized = session?.token && auth.valid(session)
    ? request.clone({ setHeaders: { Authorization: `${session.tokenType} ${session.token}` } }) : request;
  return next(authorized).pipe(catchError((error) => {
    if (error.status === 401) { auth.logout(); void router.navigate(['/customer/login'], { queryParams: { returnUrl: router.url } }); }
    return throwError(() => error);
  }));
};
