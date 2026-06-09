import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ReservationResponse } from '../reservations/reservation.models';
import { StartRentalRequest } from './rental.models';

export interface StartRentalDialogData {
  reservations: ReservationResponse[];
}

@Component({
  selector: 'acr-start-rental-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './start-rental-dialog.component.html',
  styleUrl: './rental-dialogs.scss'
})
export class StartRentalDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<StartRentalDialogComponent, StartRentalRequest>);
  readonly data = inject<StartRentalDialogData>(MAT_DIALOG_DATA);

  readonly form = this.formBuilder.nonNullable.group({
    reservationId: [0, [Validators.required, Validators.min(1)]],
    actualPickupDateTime: ['', [Validators.required]],
    startMileage: [0, [Validators.required, Validators.min(0)]]
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.getRawValue());
  }
}
