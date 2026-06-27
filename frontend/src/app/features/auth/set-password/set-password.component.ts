import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'acr-set-password',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <section class="set-password">
      <div class="panel">
        <h1>Set your password</h1>
        <p>Choose a password for your staff account. This invitation link can be used once.</p>

        @if (error()) {
          <div class="message message--error">{{ error() }}</div>
        }
        @if (success()) {
          <div class="message message--success">{{ success() }}</div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline">
            <mat-label>New password</mat-label>
            <input matInput type="password" autocomplete="new-password" formControlName="newPassword">
            @if (form.controls.newPassword.hasError('required')) {
              <mat-error>Password is required</mat-error>
            }
            @if (form.controls.newPassword.hasError('minlength')) {
              <mat-error>Password must be at least 8 characters</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Confirm password</mat-label>
            <input matInput type="password" autocomplete="new-password" formControlName="confirmPassword">
            @if (passwordMismatch()) {
              <mat-error>Passwords do not match</mat-error>
            }
          </mat-form-field>

          <button mat-flat-button color="primary" type="submit" [disabled]="loading() || !token">
            {{ loading() ? 'Saving...' : 'Set password' }}
          </button>
        </form>

        <a routerLink="/login">Back to sign in</a>
      </div>
    </section>
  `,
  styles: [`
    .set-password{min-height:100vh;display:grid;place-items:center;background:#f4f7fb;padding:24px}
    .panel{width:min(460px,100%);background:#fff;border:1px solid #dce2e9;border-radius:8px;padding:30px;box-shadow:0 8px 22px rgba(15,23,42,.06)}
    h1{margin:0 0 8px;font-size:26px}
    p{margin:0 0 22px;color:#59687b;line-height:1.5}
    form{display:flex;flex-direction:column;gap:10px}
    button{height:48px}
    a{display:block;margin-top:18px;color:#1261b5;font-weight:700;text-align:center;text-decoration:none}
    .message{border-radius:6px;padding:10px 12px;margin-bottom:14px;font-size:14px}
    .message--error{background:#fff1f1;color:#9c1b1b;border:1px solid #f2b9b9}
    .message--success{background:#ecfdf3;color:#166534;border:1px solid #bbf0cd}
  `]
})
export class SetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  readonly token = this.route.snapshot.queryParamMap.get('token') || '';
  readonly loading = signal(false);
  readonly error = signal(this.token ? '' : 'Invitation token is missing. Please request a new invitation.');
  readonly success = signal('');

  readonly form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  passwordMismatch(): boolean {
    return this.form.controls.confirmPassword.touched
      && this.form.controls.newPassword.value !== this.form.controls.confirmPassword.value;
  }

  submit(): void {
    if (this.form.invalid || this.passwordMismatch() || !this.token) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.success.set('');
    this.authService.setPassword({
      token: this.token,
      newPassword: this.form.controls.newPassword.value
    }).subscribe({
      next: (response) => {
        this.success.set(response.message);
        setTimeout(() => void this.router.navigate(['/login']), 1200);
      },
      error: (apiError) => {
        this.error.set(apiError.error?.message || 'Password could not be set. The link may be expired.');
        this.loading.set(false);
      }
    });
  }
}
