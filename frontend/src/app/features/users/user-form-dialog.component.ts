import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { UserRole } from '../../core/auth/auth.models';
import { TenantResponse } from '../tenants/tenant.models';
import { CreateUserRequest, UpdateUserRequest, UserResponse } from './user.models';

export interface UserFormDialogData {
  currentRole: UserRole;
  tenants: TenantResponse[];
  user: UserResponse | null;
}

export type UserFormDialogResult = CreateUserRequest | UpdateUserRequest;

@Component({
  selector: 'acr-user-form-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './user-form-dialog.component.html',
  styleUrl: './user-form-dialog.component.scss'
})
export class UserFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<UserFormDialogComponent, UserFormDialogResult>);
  readonly data = inject<UserFormDialogData>(MAT_DIALOG_DATA);
  readonly isEdit = Boolean(this.data.user);
  readonly isSuperAdmin = this.data.currentRole === 'SUPER_ADMIN';
  readonly roleOptions: UserRole[] = this.isSuperAdmin ? ['TENANT_ADMIN', 'TENANT_STAFF'] : ['TENANT_STAFF'];

  readonly form = this.formBuilder.nonNullable.group({
    firstName: [this.data.user?.firstName ?? '', [Validators.required]],
    lastName: [this.data.user?.lastName ?? '', [Validators.required]],
    email: [this.data.user?.email ?? '', [Validators.required, Validators.email]],
    password: ['', this.isEdit ? [Validators.minLength(8)] : []],
    role: [this.data.user?.role ?? this.roleOptions[0], [Validators.required]],
    tenantId: [this.data.user?.tenantId ?? this.data.tenants[0]?.id ?? 0],
    active: [this.data.user?.active ?? true]
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const password = value.password.trim();
    const result: UserFormDialogResult = this.isEdit
      ? {
          firstName: value.firstName,
          lastName: value.lastName,
          email: value.email,
          password: password || null,
          role: value.role,
          active: value.active
        }
      : {
          firstName: value.firstName,
          lastName: value.lastName,
          email: value.email,
          role: value.role,
          tenantId: this.isSuperAdmin ? value.tenantId : null
        };

    this.dialogRef.close(result);
  }
}
