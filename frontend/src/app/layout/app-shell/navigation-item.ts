import { UserRole } from '../../core/auth/auth.models';

export interface NavigationItem {
  label: string;
  icon: string;
  route: string;
  roles?: UserRole[];
}

export const NAVIGATION_ITEMS: NavigationItem[] = [
  { label: 'Dashboard', icon: 'dashboard', route: '/app/dashboard' },
  { label: 'Vehicles', icon: 'directions_car', route: '/app/vehicles' },
  { label: 'Reservations', icon: 'event_available', route: '/app/reservations' },
  { label: 'Rentals', icon: 'assignment_turned_in', route: '/app/rentals' },
  { label: 'Payments', icon: 'payments', route: '/app/payments' },
  { label: 'Invoices', icon: 'receipt_long', route: '/app/invoices' },
  { label: 'Reports', icon: 'bar_chart', route: '/app/reports' },
  { label: 'AI Pricing', icon: 'insights', route: '/app/ai-pricing' },
  { label: 'Notifications', icon: 'notifications', route: '/app/notifications' },
  {
    label: 'Tenant Settings',
    icon: 'tune',
    route: '/app/tenant-settings',
    roles: ['TENANT_ADMIN', 'TENANT_STAFF']
  },
  {
    label: 'Insurance Packages',
    icon: 'verified_user',
    route: '/app/insurance-packages',
    roles: ['SUPER_ADMIN', 'TENANT_ADMIN']
  },
  {
    label: 'Users',
    icon: 'group',
    route: '/app/users',
    roles: ['SUPER_ADMIN', 'TENANT_ADMIN']
  },
  {
    label: 'Tenants',
    icon: 'business',
    route: '/app/tenants',
    roles: ['SUPER_ADMIN']
  }
];
