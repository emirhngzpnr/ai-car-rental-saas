import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStorageService } from './auth-storage.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const session = inject(AuthStorageService).getSession();

  if (!session?.token) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `${session.tokenType} ${session.token}`
      }
    })
  );
};
