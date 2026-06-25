import { Validators } from '@angular/forms';

export const EMAIL_MAX_LENGTH = 254;
export const EMAIL_PATTERN =
  /^[A-Za-z0-9._%+-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/;

export const EMAIL_VALIDATORS = [
  Validators.required,
  Validators.maxLength(EMAIL_MAX_LENGTH),
  Validators.pattern(EMAIL_PATTERN),
];
