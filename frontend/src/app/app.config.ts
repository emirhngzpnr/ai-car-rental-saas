import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { customerAuthInterceptor } from './core/customer-auth/customer-auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { CustomerAuthService } from './core/customer-auth/customer-auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideAnimations(),
    provideHttpClient(withInterceptors([authInterceptor, customerAuthInterceptor])),
    provideRouter(routes, withComponentInputBinding()),
    provideAppInitializer(() => {
      const authService = inject(AuthService);
      const customerAuthService = inject(CustomerAuthService);
      return Promise.allSettled([
        authService.initialize(),
        customerAuthService.initialize()
      ]).then(() => undefined);
    })
  ]
};
