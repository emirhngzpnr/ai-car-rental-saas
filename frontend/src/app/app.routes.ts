import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { roleGuard } from './core/auth/role.guard';
import { AppShellComponent } from './layout/app-shell/app-shell.component';
import { LoginComponent } from './features/auth/login/login.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { VehiclesComponent } from './features/vehicles/vehicles.component';
import { PlaceholderPageComponent } from './features/placeholder-page/placeholder-page.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [guestGuard]
  },
  {
    path: 'app',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'vehicles', component: VehiclesComponent },
      {
        path: 'reservations',
        component: PlaceholderPageComponent,
        data: { title: 'Reservations', description: 'Reservation workflows will use live backend data for booking, confirmation and cancellation.' }
      },
      {
        path: 'rentals',
        component: PlaceholderPageComponent,
        data: { title: 'Rentals', description: 'Rental start and completion operations will be added with mileage and deposit controls.' }
      },
      {
        path: 'payments',
        component: PlaceholderPageComponent,
        data: { title: 'Payments', description: 'Payment operations will be enabled after tenant ownership hardening is completed on the backend.' }
      },
      {
        path: 'ai-pricing',
        component: PlaceholderPageComponent,
        data: { title: 'AI Pricing', description: 'Decision support recommendations will be presented with approval controls and confidence indicators.' }
      },
      {
        path: 'notifications',
        component: PlaceholderPageComponent,
        data: { title: 'Notifications', description: 'Operational notification history and delivery status will be connected to the backend notification API.' }
      },
      {
        path: 'tenant-settings',
        component: PlaceholderPageComponent,
        data: { title: 'Tenant Settings', description: 'Tenant-level configuration will be managed here for pricing, automation and operational defaults.' }
      },
      {
        path: 'users',
        component: PlaceholderPageComponent,
        canActivate: [roleGuard],
        data: {
          roles: ['SUPER_ADMIN', 'TENANT_ADMIN'],
          title: 'Users',
          description: 'User management will support tenant staff operations with role-aware actions.'
        }
      },
      {
        path: 'tenants',
        component: PlaceholderPageComponent,
        canActivate: [roleGuard],
        data: {
          roles: ['SUPER_ADMIN'],
          title: 'Tenants',
          description: 'Tenant administration is reserved for platform operators.'
        }
      },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '', pathMatch: 'full', redirectTo: 'app/dashboard' },
  { path: '**', redirectTo: 'app/dashboard' }
];
