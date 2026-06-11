import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CreateTenantRequest, TenantResponse, UpdateTenantRequest } from './tenant.models';

export interface TenantFormDialogData {
  tenant: TenantResponse | null;
}

export type TenantFormDialogResult = CreateTenantRequest | UpdateTenantRequest;

@Component({
  selector: 'acr-tenant-form-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './tenant-form-dialog.component.html',
  styleUrl: './tenant-form-dialog.component.scss'
})
export class TenantFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<TenantFormDialogComponent, TenantFormDialogResult>);
  readonly data = inject<TenantFormDialogData>(MAT_DIALOG_DATA);
  readonly isEdit = Boolean(this.data.tenant);

  readonly form = this.formBuilder.nonNullable.group({
    companyName: [this.data.tenant?.companyName ?? '', [Validators.required]],
    subDomain: [this.data.tenant?.subDomain ?? '', [Validators.required]],
    email: [this.data.tenant?.email ?? '', [Validators.required, Validators.email]],
    phoneNumber: [this.data.tenant?.phoneNumber ?? '', [Validators.required]],
    active: [this.data.tenant?.active ?? true]
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.dialogRef.close(this.isEdit ? value : {
      companyName: value.companyName,
      subDomain: value.subDomain,
      email: value.email,
      phoneNumber: value.phoneNumber
    });
  }
}
