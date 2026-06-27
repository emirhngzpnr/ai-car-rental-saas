import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CustomerAuthService } from '../../core/customer-auth/customer-auth.service';

@Component({
  selector: 'acr-customer-verify-email',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <section class="auth-state">
      <mat-icon>{{statusIcon()}}</mat-icon>
      <h1>{{title()}}</h1>
      <p>{{message()}}</p>
      @if (email()) {
        <div class="email">{{email()}}</div>
      }
      @if (pending()) {
        <button mat-stroked-button type="button" [disabled]="loading()" (click)="resend()">
          {{loading() ? 'Sending...' : 'Resend verification email'}}
        </button>
      }
      @if (!pending()) {
        <a mat-flat-button color="primary" routerLink="/customer/login">Continue to sign in</a>
      }
    </section>
  `,
  styles: [`.auth-state{max-width:560px;margin:56px auto;background:#fff;border:1px solid #dce2e9;border-radius:8px;padding:32px;text-align:center}.auth-state mat-icon{width:44px;height:44px;font-size:44px;color:#1261b5}.auth-state h1{font-size:26px;margin:14px 0 8px}.auth-state p{color:#52627a;line-height:1.5}.email{display:inline-flex;background:#f1f5f9;border-radius:4px;padding:8px 10px;margin:8px 0 18px;font-weight:700}.auth-state a,.auth-state button{margin-top:10px}`]
})
export class CustomerVerifyEmailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(CustomerAuthService);
  readonly loading = signal(false);
  readonly pending = signal(this.route.snapshot.queryParamMap.get('pending') === 'true');
  readonly email = signal(this.route.snapshot.queryParamMap.get('email') || '');
  readonly title = signal(this.pending() ? 'Check your email' : 'Verifying your email');
  readonly message = signal(this.pending() ? 'We sent a verification link. You can sign in after confirming your email address.' : 'Please wait while we confirm your account.');
  readonly statusIcon = signal(this.pending() ? 'mark_email_unread' : 'verified');

  constructor() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!this.pending() && token) {
      this.auth.verifyEmail(token).subscribe({
        next: (response) => {
          this.title.set('Email verified');
          this.message.set(response.message);
          this.statusIcon.set('verified');
        },
        error: (error) => {
          this.title.set('Verification link is invalid');
          this.message.set(error.error?.message || 'This verification link is invalid or expired.');
          this.statusIcon.set('error_outline');
        }
      });
    } else if (!this.pending()) {
      this.title.set('Verification token is missing');
      this.message.set('Open the verification link from your email or request a new one.');
      this.statusIcon.set('error_outline');
    }
  }

  resend(): void {
    if (!this.email()) return;
    this.loading.set(true);
    this.auth.resendVerification({ email: this.email() }).subscribe({
      next: (response) => this.message.set(response.message),
      error: (error) => this.message.set(error.error?.message || 'Verification email could not be sent.'),
      complete: () => this.loading.set(false)
    });
  }
}
