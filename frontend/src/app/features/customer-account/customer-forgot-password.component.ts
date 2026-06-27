import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CustomerAuthService } from '../../core/customer-auth/customer-auth.service';
import { EMAIL_MAX_LENGTH, EMAIL_VALIDATORS } from '../../core/validation/email.validation';

@Component({
  selector: 'acr-customer-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <section class="access-panel">
      <h1>Reset your password</h1>
      <p>Enter your account email. If it exists, we will send a reset link.</p>
      @if (message()) { <div class="notice">{{message()}}</div> }
      <form [formGroup]="form" (ngSubmit)="submit()">
        <mat-form-field appearance="outline">
          <mat-label>Email</mat-label>
          <input matInput type="email" autocomplete="email" [maxLength]="emailMaxLength" formControlName="email">
          @if (form.controls.email.hasError('required')) { <mat-error>Email is required</mat-error> }
          @else if (form.controls.email.invalid) { <mat-error>Enter a valid email address</mat-error> }
        </mat-form-field>
        <button mat-flat-button color="primary" [disabled]="loading()">{{loading() ? 'Sending...' : 'Send reset link'}}</button>
        <a routerLink="/customer/login">Back to sign in</a>
      </form>
    </section>
  `,
  styles: [`.access-panel{max-width:520px;margin:48px auto;background:#fff;border:1px solid #dce2e9;border-radius:8px;padding:30px}.access-panel h1{font-size:26px;margin:0 0 8px}.access-panel p{color:#52627a}.access-panel form{display:flex;flex-direction:column;gap:10px;margin-top:20px}.access-panel a{align-self:center;color:#1261b5;font-weight:700}.notice{background:#eef7f0;color:#27673c;padding:11px;border-radius:5px;margin-top:16px}`]
})
export class CustomerForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(CustomerAuthService);
  readonly emailMaxLength = EMAIL_MAX_LENGTH;
  readonly loading = signal(false);
  readonly message = signal('');
  readonly form = this.fb.group({ email: ['', EMAIL_VALIDATORS] });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.message.set('');
    this.auth.forgotPassword({ email: this.form.getRawValue().email! }).subscribe({
      next: (response) => this.message.set(response.message),
      error: (error) => this.message.set(error.error?.message || 'Reset email could not be sent.'),
      complete: () => this.loading.set(false)
    });
  }
}
