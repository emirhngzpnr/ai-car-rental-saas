import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { roleGuard } from './core/auth/role.guard';
import { AppShellComponent } from './layout/app-shell/app-shell.component';
import { LoginComponent } from './features/auth/login/login.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { VehiclesComponent } from './features/vehicles/vehicles.component';
import { ReservationsComponent } from './features/reservations/reservations.component';
import { RentalsComponent } from './features/rentals/rentals.component';
import { PaymentsComponent } from './features/payments/payments.component';
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
      { path: 'reservations', component: ReservationsComponent },
      { path: 'rentals', component: RentalsComponent },
      { path: 'payments', component: PaymentsComponent },
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
