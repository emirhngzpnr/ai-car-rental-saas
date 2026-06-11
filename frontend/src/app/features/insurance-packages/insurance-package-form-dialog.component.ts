import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CreateInsurancePackageRequest, InsurancePackageResponse, InsurancePackageType, UpdateInsurancePackageRequest } from './insurance-package.models';

export interface InsurancePackageFormDialogData {
  tenantId: number;
  insurancePackage: InsurancePackageResponse | null;
}

export type InsurancePackageFormDialogResult = CreateInsurancePackageRequest | UpdateInsurancePackageRequest;

@Component({
  selector: 'acr-insurance-package-form-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './insurance-package-form-dialog.component.html',
  styleUrl: './insurance-package-form-dialog.component.scss'
})
export class InsurancePackageFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<InsurancePackageFormDialogComponent, InsurancePackageFormDialogResult>);
  readonly data = inject<InsurancePackageFormDialogData>(MAT_DIALOG_DATA);
  readonly types: InsurancePackageType[] = ['BASIC', 'STANDARD', 'PREMIUM', 'FULL_COVERAGE'];
  readonly isEdit = Boolean(this.data.insurancePackage);

  readonly form = this.formBuilder.nonNullable.group({
    type: [this.data.insurancePackage?.type ?? 'BASIC' as InsurancePackageType, [Validators.required]],
    name: [this.data.insurancePackage?.name ?? '', [Validators.required, Validators.maxLength(100)]],
    coverageDescription: [this.data.insurancePackage?.coverageDescription ?? '', [Validators.required, Validators.maxLength(1000)]],
    dailyPrice: [Number(this.data.insurancePackage?.dailyPrice ?? 0), [Validators.required, Validators.min(0)]],
    active: [this.data.insurancePackage?.active ?? true]
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const result: InsurancePackageFormDialogResult = this.isEdit
      ? value
      : {
          tenantId: this.data.tenantId,
          type: value.type,
          name: value.name,
          coverageDescription: value.coverageDescription,
          dailyPrice: value.dailyPrice
        };

    this.dialogRef.close(result);
  }
}
