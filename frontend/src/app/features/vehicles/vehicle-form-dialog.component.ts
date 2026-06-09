import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CreateVehicleRequest, UpdateVehicleRequest, VehicleResponse, VehicleStatus } from './vehicle.models';

export interface VehicleFormDialogData {
  vehicle: VehicleResponse | null;
}

export type VehicleFormDialogResult = CreateVehicleRequest | UpdateVehicleRequest;

@Component({
  selector: 'acr-vehicle-form-dialog',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule
  ],
  templateUrl: './vehicle-form-dialog.component.html',
  styleUrl: './vehicle-form-dialog.component.scss'
})
export class VehicleFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<VehicleFormDialogComponent, VehicleFormDialogResult>);
  readonly data = inject<VehicleFormDialogData>(MAT_DIALOG_DATA);
  readonly statuses: VehicleStatus[] = ['AVAILABLE', 'RENTED', 'MAINTENANCE', 'PASSIVE'];
  readonly isEdit = Boolean(this.data.vehicle);

  readonly form = this.formBuilder.nonNullable.group({
    brand: [this.data.vehicle?.brand ?? '', [Validators.required]],
    model: [this.data.vehicle?.model ?? '', [Validators.required]],
    plateNumber: [this.data.vehicle?.plateNumber ?? '', [Validators.required]],
    productionYear: [this.data.vehicle?.productionYear ?? new Date().getFullYear(), [Validators.required, Validators.min(2000)]],
    currentMileage: [this.data.vehicle?.currentMileage ?? 0, [Validators.required, Validators.min(0)]],
    dailyPrice: [Number(this.data.vehicle?.dailyPrice ?? 0), [Validators.required, Validators.min(0.01)]],
    dailyKmLimit: [this.data.vehicle?.dailyKmLimit ?? 250, [Validators.required, Validators.min(1)]],
    extraKmPricePerKm: [Number(this.data.vehicle?.extraKmPricePerKm ?? 0), [Validators.required, Validators.min(0.01)]],
    status: [this.data.vehicle?.status ?? 'AVAILABLE' as VehicleStatus, [Validators.required]],
    active: [this.data.vehicle?.active ?? true]
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const result: VehicleFormDialogResult = this.isEdit
      ? value
      : {
          brand: value.brand,
          model: value.model,
          plateNumber: value.plateNumber,
          productionYear: value.productionYear,
          currentMileage: value.currentMileage,
          dailyPrice: value.dailyPrice,
          dailyKmLimit: value.dailyKmLimit,
          extraKmPricePerKm: value.extraKmPricePerKm,
          status: value.status
        };

    this.dialogRef.close(result);
  }
}
