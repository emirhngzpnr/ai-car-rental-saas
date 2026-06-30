import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function passwordsMatchValidator(
  passwordControlName = 'newPassword',
  confirmPasswordControlName = 'confirmPassword'
): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const passwordControl = control.get(passwordControlName);
    const confirmPasswordControl = control.get(confirmPasswordControlName);

    if (!passwordControl || !confirmPasswordControl) {
      return null;
    }

    const password = passwordControl.value;
    const confirmPassword = confirmPasswordControl.value;
    const existingErrors = confirmPasswordControl.errors ?? {};
    const hasMismatch = Boolean(password && confirmPassword && password !== confirmPassword);

    if (hasMismatch) {
      confirmPasswordControl.setErrors({ ...existingErrors, passwordMismatch: true });
      return { passwordMismatch: true };
    }

    if (existingErrors['passwordMismatch']) {
      const { passwordMismatch, ...remainingErrors } = existingErrors;
      confirmPasswordControl.setErrors(Object.keys(remainingErrors).length ? remainingErrors : null);
    }

    return null;
  };
}
