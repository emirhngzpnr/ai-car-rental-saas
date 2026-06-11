import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { VehicleResponse } from '../vehicles/vehicle.models';
import { VehicleService } from '../vehicles/vehicle.service';
import { CreateReservationRequest } from './reservation.models';

export interface ReservationFormDialogData {
  vehicles: VehicleResponse[];
}

@Component({
  selector: 'acr-reservation-form-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  templateUrl: './reservation-form-dialog.component.html',
  styleUrl: './reservation-form-dialog.component.scss'
})
export class ReservationFormDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<ReservationFormDialogComponent, CreateReservationRequest>);
  private readonly vehicleService = inject(VehicleService);
  readonly data = inject<ReservationFormDialogData>(MAT_DIALOG_DATA);
  readonly availableVehicles = signal<VehicleResponse[]>(this.data.vehicles);
  readonly availabilityLoading = signal(false);
  readonly availabilityError = signal('');

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

  checkAvailability(): void {
    const pickupDateTime = this.form.controls.pickupDateTime.value;
    const returnDateTime = this.form.controls.returnDateTime.value;

    if (!pickupDateTime || !returnDateTime) {
      this.form.controls.pickupDateTime.markAsTouched();
      this.form.controls.returnDateTime.markAsTouched();
      return;
    }

    this.availabilityLoading.set(true);
    this.availabilityError.set('');
    this.vehicleService.getAvailableVehicles(pickupDateTime, returnDateTime).subscribe({
      next: (vehicles) => {
        this.availableVehicles.set(vehicles);
        this.form.controls.vehicleId.setValue(0);
      },
      error: (error: HttpErrorResponse) => {
        this.availabilityError.set(error.error?.message || 'Available vehicles could not be loaded.');
        this.availabilityLoading.set(false);
      },
      complete: () => this.availabilityLoading.set(false)
    });
  }

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
