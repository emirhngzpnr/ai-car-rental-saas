import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { roleGuard } from './core/auth/role.guard';
import { AppShellComponent } from './layout/app-shell/app-shell.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { VehiclesComponent } from './features/vehicles/vehicles.component';
import { ReservationsComponent } from './features/reservations/reservations.component';
import { RentalsComponent } from './features/rentals/rentals.component';
import { PaymentsComponent } from './features/payments/payments.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then((component) => component.LoginComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'app',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'vehicles', component: VehiclesComponent },
      { path: 'reservations', component: ReservationsComponent },
      { path: 'rentals', component: RentalsComponent },
      { path: 'payments', component: PaymentsComponent },
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
  { path: '', pathMatch: 'full', redirectTo: 'app/dashboard' },
  { path: '**', redirectTo: 'app/dashboard' }
];
