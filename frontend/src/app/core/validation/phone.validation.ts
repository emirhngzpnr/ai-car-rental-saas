import { Validators } from '@angular/forms';

export const PHONE_MAX_LENGTH = 20;
export const PHONE_PATTERN = /^(?=(?:\D*\d){7,15}\D*$)\+?[0-9() -]+$/;
export const PHONE_VALIDATORS = [
  Validators.required,
  Validators.maxLength(PHONE_MAX_LENGTH),
  Validators.pattern(PHONE_PATTERN)
];
