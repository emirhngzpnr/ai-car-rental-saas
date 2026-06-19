import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { CustomerAuthService } from './customer-auth.service';

export const customerAuthGuard: CanActivateFn = (_route, state) => {
  const auth = inject(CustomerAuthService);
  return auth.isAuthenticated() ? true : inject(Router).createUrlTree(['/customer/login'], { queryParams: { returnUrl: state.url } });
};
