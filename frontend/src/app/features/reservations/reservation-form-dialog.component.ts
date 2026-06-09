import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { VehicleResponse } from '../vehicles/vehicle.models';
import { CreateReservationRequest } from './reservation.models';

export interface ReservationFormDialogData {
  vehicles: VehicleResponse[];
}

@Component({
  selector: 'acr-reservation-form-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './reservation-form-dialog.component.html',
  styleUrl: './reservation-form-dialog.component.scss'
})
export class ReservationFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ReservationFormDialogComponent, CreateReservationRequest>);
  readonly data = inject<ReservationFormDialogData>(MAT_DIALOG_DATA);

  readonly form = this.formBuilder.nonNullable.group({
    vehicleId: [0, [Validators.required, Validators.min(1)]],
    customerFullName: ['', [Validators.required]],
    customerPhone: ['', [Validators.required]],
    customerEmail: ['', [Validators.required, Validators.email]],
    customerIdentityNumber: ['', [Validators.required]],
    pickupDateTime: ['', [Validators.required]],
    returnDateTime: ['', [Validators.required]],
    insurancePackageId: ['']
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.dialogRef.close({
      vehicleId: value.vehicleId,
      customerFullName: value.customerFullName,
      customerPhone: value.customerPhone,
      customerEmail: value.customerEmail,
      customerIdentityNumber: value.customerIdentityNumber,
      pickupDateTime: value.pickupDateTime,
      returnDateTime: value.returnDateTime,
      insurancePackageId: value.insurancePackageId ? Number(value.insurancePackageId) : null
    });
  }
}
