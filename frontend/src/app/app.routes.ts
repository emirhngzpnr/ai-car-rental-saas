import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { roleGuard } from './core/auth/role.guard';
import { customerAuthGuard } from './core/customer-auth/customer-auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((component) => component.LoginComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'set-password',
    loadComponent: () => import('./features/auth/set-password/set-password.component').then((component) => component.SetPasswordComponent)
  },
  {
    path: 'rent',
    loadComponent: () => import('./layout/customer-shell/customer-shell.component').then(c => c.CustomerShellComponent),
    children: [
      { path: '', loadComponent: () => import('./features/marketplace/marketplace-search.component').then(c => c.MarketplaceSearchComponent) },
      { path: 'vehicles/:vehicleId', loadComponent: () => import('./features/marketplace/marketplace-detail.component').then(c => c.MarketplaceDetailComponent) },
      { path: 'checkout/:vehicleId', loadComponent: () => import('./features/marketplace/marketplace-checkout.component').then(c => c.MarketplaceCheckoutComponent) },
      { path: 'reservation/success', loadComponent: () => import('./features/marketplace/reservation-success.component').then(c => c.ReservationSuccessComponent) },
      { path: 'track', loadComponent: () => import('./features/marketplace/reservation-tracking.component').then(c => c.ReservationTrackingComponent) }
    ]
  },
  {
    path: 'customer',
    loadComponent: () => import('./layout/customer-shell/customer-shell.component').then(c => c.CustomerShellComponent),
    children: [
      { path: 'login', data: { mode: 'login' }, loadComponent: () => import('./features/customer-account/customer-access.component').then(c => c.CustomerAccessComponent) },
      { path: 'register', data: { mode: 'register' }, loadComponent: () => import('./features/customer-account/customer-access.component').then(c => c.CustomerAccessComponent) },
      { path: 'verify-email', loadComponent: () => import('./features/customer-account/customer-verify-email.component').then(c => c.CustomerVerifyEmailComponent) },
      { path: 'forgot-password', loadComponent: () => import('./features/customer-account/customer-forgot-password.component').then(c => c.CustomerForgotPasswordComponent) },
      { path: 'reset-password', loadComponent: () => import('./features/customer-account/customer-reset-password.component').then(c => c.CustomerResetPasswordComponent) },
      { path: 'account/profile', canActivate: [customerAuthGuard], loadComponent: () => import('./features/customer-account/customer-profile.component').then(c => c.CustomerProfileComponent) },
      { path: 'account/reservations', canActivate: [customerAuthGuard], loadComponent: () => import('./features/customer-account/customer-reservations.component').then(c => c.CustomerReservationsComponent) },
      { path: 'account/reservations/:reservationCode', canActivate: [customerAuthGuard], loadComponent: () => import('./features/customer-account/customer-reservation-detail.component').then(c => c.CustomerReservationDetailComponent) }
    ]
  },
  {
    path: 'app',
    loadComponent: () => import('./layout/app-shell/app-shell.component').then((component) => component.AppShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then((component) => component.DashboardComponent)
      },
      {
        path: 'vehicles',
        loadComponent: () => import('./features/vehicles/vehicles.component').then((component) => component.VehiclesComponent)
      },
      {
        path: 'reservations',
        loadComponent: () => import('./features/reservations/reservations.component').then((component) => component.ReservationsComponent)
      },
      {
        path: 'rentals',
        loadComponent: () => import('./features/rentals/rentals.component').then((component) => component.RentalsComponent)
      },
      {
        path: 'payments',
        loadComponent: () => import('./features/payments/payments.component').then((component) => component.PaymentsComponent)
      },
      {
        path: 'invoices',
        loadComponent: () => import('./features/invoices/invoices.component').then((component) => component.InvoicesComponent)
      },
      {
        path: 'reports',
        loadComponent: () => import('./features/reports/reports.component').then((component) => component.ReportsComponent)
      },
      {
        path: 'ai-pricing',
        loadComponent: () => import('./features/ai-pricing/ai-pricing.component').then((component) => component.AiPricingComponent)
      },
      {
        path: 'notifications',
        loadComponent: () => import('./features/notifications/notifications.component').then((component) => component.NotificationsComponent)
      },
      {
        path: 'tenant-settings',
        loadComponent: () => import('./features/tenant-settings/tenant-settings.component').then((component) => component.TenantSettingsComponent),
        canActivate: [roleGuard],
        data: { roles: ['TENANT_ADMIN', 'TENANT_STAFF'] }
      },
      {
        path: 'knowledge-base',
        loadComponent: () => import('./features/knowledge-base/knowledge-base.component').then((component) => component.KnowledgeBaseComponent),
        canActivate: [roleGuard],
        data: { roles: ['TENANT_ADMIN', 'TENANT_STAFF'] }
      },
      {
        path: 'insurance-packages',
        loadComponent: () => import('./features/insurance-packages/insurance-packages.component').then((component) => component.InsurancePackagesComponent),
        canActivate: [roleGuard],
        data: { roles: ['SUPER_ADMIN', 'TENANT_ADMIN'] }
      },
      {
        path: 'users',
        loadComponent: () => import('./features/users/users.component').then((component) => component.UsersComponent),
        canActivate: [roleGuard],
        data: {
          roles: ['SUPER_ADMIN', 'TENANT_ADMIN']
        }
      },
      {
        path: 'tenants',
        loadComponent: () => import('./features/tenants/tenants.component').then((component) => component.TenantsComponent),
        canActivate: [roleGuard],
        data: {
          roles: ['SUPER_ADMIN']
        }
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '', pathMatch: 'full', redirectTo: 'rent' },
  { path: '**', redirectTo: 'rent' }
];
