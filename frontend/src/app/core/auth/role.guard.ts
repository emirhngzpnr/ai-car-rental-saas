import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { UserRole } from './auth.models';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route): boolean | UrlTree => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const roles = route.data['roles'] as UserRole[] | undefined;

  if (!roles?.length || authService.hasAnyRole(roles)) {
    return true;
  }

  return router.createUrlTree(['/app/dashboard']);
};
