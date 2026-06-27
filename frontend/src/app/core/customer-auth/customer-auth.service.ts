import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, firstValueFrom, map, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CustomerAuthResponse, CustomerEmailRequest, CustomerLoginRequest, CustomerMessageResponse, CustomerRegisterRequest, CustomerRegistrationResponse, CustomerResetPasswordRequest, CustomerSession } from './customer-auth.models';

@Injectable({ providedIn: 'root' })
export class CustomerAuthService {
  private readonly http = inject(HttpClient);
  private readonly sessionSignal = signal<CustomerSession | null>(null);
  private refreshRequest$: Observable<CustomerSession> | null = null;
  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.valid(this.sessionSignal()));

  login(request: CustomerLoginRequest): Observable<CustomerAuthResponse> {
    return this.http.post<CustomerAuthResponse>(`${environment.apiUrl}/api/customer/auth/login`, request, { withCredentials: true })
      .pipe(tap((response) => this.sessionSignal.set(response)));
  }
  register(request: CustomerRegisterRequest): Observable<CustomerRegistrationResponse> {
    return this.http.post<CustomerRegistrationResponse>(`${environment.apiUrl}/api/customer/auth/register`, request, { withCredentials: true });
  }
  verifyEmail(token: string): Observable<CustomerMessageResponse> {
    return this.http.post<CustomerMessageResponse>(`${environment.apiUrl}/api/customer/auth/verify-email`, { token }, { withCredentials: true });
  }
  resendVerification(request: CustomerEmailRequest): Observable<CustomerMessageResponse> {
    return this.http.post<CustomerMessageResponse>(`${environment.apiUrl}/api/customer/auth/resend-verification`, request, { withCredentials: true });
  }
  forgotPassword(request: CustomerEmailRequest): Observable<CustomerMessageResponse> {
    return this.http.post<CustomerMessageResponse>(`${environment.apiUrl}/api/customer/auth/forgot-password`, request, { withCredentials: true });
  }
  resetPassword(request: CustomerResetPasswordRequest): Observable<CustomerMessageResponse> {
    return this.http.post<CustomerMessageResponse>(`${environment.apiUrl}/api/customer/auth/reset-password`, request, { withCredentials: true });
  }
  refresh(): Observable<CustomerSession> {
    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }

    this.refreshRequest$ = this.http
      .post<CustomerAuthResponse>(`${environment.apiUrl}/api/customer/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        map((response) => response as CustomerSession),
        tap((response) => this.sessionSignal.set(response)),
        finalize(() => {
          this.refreshRequest$ = null;
        }),
        shareReplay({ bufferSize: 1, refCount: false })
      );

    return this.refreshRequest$;
  }
  async initialize(): Promise<void> {
    await firstValueFrom(
      this.refresh().pipe(
        map(() => undefined),
        catchError(() => {
          this.sessionSignal.set(null);
          return of(undefined);
        })
      )
    );
  }
  logout(): void {
    this.sessionSignal.set(null);
    this.http.post<void>(`${environment.apiUrl}/api/customer/auth/logout`, {}, { withCredentials: true })
      .pipe(catchError(() => of(undefined)))
      .subscribe();
  }
  valid(session: CustomerSession | null): boolean {
    if (!session?.token) return false;
    try {
      const part = session.token.split('.')[1];
      const normalized = part.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, '=')));
      return typeof payload.exp === 'number' && payload.exp * 1000 > Date.now();
    } catch { return false; }
  }
}
