import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CompleteRentalRequest, RentalResponse } from './rental.models';

export interface CompleteRentalDialogData {
  rental: RentalResponse;
}

@Component({
  selector: 'acr-complete-rental-dialog',
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './complete-rental-dialog.component.html',
  styleUrl: './rental-dialogs.scss'
})
export class CompleteRentalDialogComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly dialogRef = inject(MatDialogRef<CompleteRentalDialogComponent, CompleteRentalRequest>);
  readonly data = inject<CompleteRentalDialogData>(MAT_DIALOG_DATA);

  readonly form = this.formBuilder.nonNullable.group({
    actualReturnDateTime: ['', [Validators.required]],
    endMileage: [this.data.rental.startMileage, [Validators.required, Validators.min(this.data.rental.startMileage)]]
  });

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.getRawValue());
  }
}
