import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { CustomerAuthService } from '../../core/customer-auth/customer-auth.service';
import { passwordsMatchValidator } from '../../shared/validators/password-match.validator';

@Component({
  selector: 'acr-customer-reset-password',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <section class="access-panel">
      <h1>Set a new password</h1>
      <p>Choose a new password for your customer account.</p>
      @if (message()) { <div class="notice">{{message()}}</div> }
      @if (error()) { <div class="error">{{error()}}</div> }
      <form [formGroup]="form" (ngSubmit)="submit()">
        <mat-form-field appearance="outline">
          <mat-label>New password</mat-label>
          <input matInput type="password" autocomplete="new-password" formControlName="newPassword">
          <mat-hint>At least 8 characters</mat-hint>
          @if (form.controls.newPassword.hasError('required')) { <mat-error>Password is required</mat-error> }
          @if (form.controls.newPassword.hasError('minlength')) { <mat-error>Password must be at least 8 characters</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Confirm password</mat-label>
          <input matInput type="password" autocomplete="new-password" formControlName="confirmPassword">
          @if (form.controls.confirmPassword.hasError('required')) { <mat-error>Password confirmation is required</mat-error> }
          @if (passwordMismatch()) { <mat-error>Passwords do not match</mat-error> }
        </mat-form-field>
        <button mat-flat-button color="primary" [disabled]="loading() || !token">{{loading() ? 'Saving...' : 'Update password'}}</button>
        <a routerLink="/customer/login">Back to sign in</a>
      </form>
    </section>
  `,
  styles: [`.access-panel{max-width:520px;margin:48px auto;background:#fff;border:1px solid #dce2e9;border-radius:8px;padding:30px}.access-panel h1{font-size:26px;margin:0 0 8px}.access-panel p{color:#52627a}.access-panel form{display:flex;flex-direction:column;gap:10px;margin-top:20px}.access-panel a{align-self:center;color:#1261b5;font-weight:700}.notice{background:#eef7f0;color:#27673c;padding:11px;border-radius:5px}.error{background:#fff1f1;color:#a11c1c;padding:11px;border-radius:5px}`]
})
export class CustomerResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(CustomerAuthService);
  readonly loading = signal(false);
  readonly message = signal('');
  readonly error = signal('');
  readonly token = this.route.snapshot.queryParamMap.get('token') || '';
  readonly form = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordsMatchValidator() });

  constructor() {
    if (!this.token) {
      this.error.set('Reset token is missing. Please request a new password reset email.');
    }
  }

  passwordMismatch(): boolean {
    const confirmPassword = this.form.controls.confirmPassword;
    return confirmPassword.hasError('passwordMismatch') && (confirmPassword.touched || confirmPassword.dirty);
  }

  submit(): void {
    if (this.form.invalid || this.passwordMismatch() || !this.token) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set('');
    this.auth.resetPassword({ token: this.token, newPassword: this.form.getRawValue().newPassword! }).subscribe({
      next: (response) => {
        this.message.set(response.message);
        setTimeout(() => void this.router.navigate(['/customer/login']), 1200);
      },
      error: (error) => {
        this.error.set(error.error?.message || 'Password could not be updated.');
        this.loading.set(false);
      }
    });
  }
}
