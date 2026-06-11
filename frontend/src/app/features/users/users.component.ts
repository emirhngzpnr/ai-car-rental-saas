import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin, of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { EmptyStateComponent } from '../../shared/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../shared/error-state/error-state.component';
import { LoadingStateComponent } from '../../shared/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../shared/page-header/page-header.component';
import { StatusBadgeComponent } from '../../shared/status-badge/status-badge.component';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';
import { TenantResponse } from '../tenants/tenant.models';
import { TenantService } from '../tenants/tenant.service';
import { CreateUserRequest, UpdateUserRequest, UserResponse } from './user.models';
import { UserService } from './user.service';
import { UserFormDialogComponent, UserFormDialogResult } from './user-form-dialog.component';

@Component({
  selector: 'acr-users',
  imports: [MatButtonModule, MatDialogModule, MatIconModule, MatTableModule, MatTooltipModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusBadgeComponent],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly tenantService = inject(TenantService);
  private readonly authService = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly confirmDialog = inject(ConfirmDialogService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly users = signal<UserResponse[]>([]);
  readonly tenants = signal<TenantResponse[]>([]);
  readonly columns = ['user', 'role', 'tenant', 'status', 'actions'];
  readonly currentRole = this.authService.currentRole();

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.error.set('');

    forkJoin({
      users: this.userService.getUsers(),
      tenants: this.currentRole === 'SUPER_ADMIN' ? this.tenantService.getTenants() : of([])
    }).subscribe({
      next: ({ users, tenants }) => {
        this.users.set(users);
        this.tenants.set(tenants);
      },
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'Users could not be loaded.');
        this.loading.set(false);
      },
      complete: () => this.loading.set(false)
    });
  }

  openCreateDialog(): void {
    if (!this.currentRole) return;
    const dialogRef = this.dialog.open(UserFormDialogComponent, {
      data: { currentRole: this.currentRole, tenants: this.tenants(), user: null },
      width: '800px',
      maxWidth: '94vw'
    });

    dialogRef.afterClosed().subscribe((result?: UserFormDialogResult) => {
      if (result) this.createUser(result as CreateUserRequest);
    });
  }

  openEditDialog(user: UserResponse): void {
    if (!this.currentRole || !this.canManageUser(user)) return;
    const dialogRef = this.dialog.open(UserFormDialogComponent, {
      data: { currentRole: this.currentRole, tenants: this.tenants(), user },
      width: '800px',
      maxWidth: '94vw'
    });

    dialogRef.afterClosed().subscribe((result?: UserFormDialogResult) => {
      if (result) this.updateUser(user.id, result as UpdateUserRequest);
    });
  }

  deactivateUser(user: UserResponse): void {
    if (!this.canManageUser(user)) return;
    this.confirmDialog.confirm({
      title: 'Deactivate user',
      message: `Deactivate user ${user.email}? The account will no longer be available for sign-in.`,
      confirmLabel: 'Deactivate',
      tone: 'danger'
    }).subscribe((confirmed) => {
      if (!confirmed) return;
      this.saving.set(true);
      this.userService.deleteUser(user.id).subscribe({
        next: () => this.loadUsers(),
        error: (error: HttpErrorResponse) => {
          this.error.set(error.error?.message || 'User could not be deactivated.');
          this.saving.set(false);
        },
        complete: () => this.saving.set(false)
      });
    });
  }

  canManageUser(user: UserResponse): boolean {
    if (this.currentRole === 'SUPER_ADMIN') {
      return user.role !== 'SUPER_ADMIN';
    }

    if (this.currentRole === 'TENANT_ADMIN') {
      return user.role === 'TENANT_STAFF';
    }

    return false;
  }

  private createUser(request: CreateUserRequest): void {
    this.saving.set(true);
    this.userService.createUser(request).subscribe({
      next: () => this.loadUsers(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'User could not be created.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }

  private updateUser(id: number, request: UpdateUserRequest): void {
    this.saving.set(true);
    this.userService.updateUser(id, request).subscribe({
      next: () => this.loadUsers(),
      error: (error: HttpErrorResponse) => {
        this.error.set(error.error?.message || 'User could not be updated.');
        this.saving.set(false);
      },
      complete: () => this.saving.set(false)
    });
  }
}
