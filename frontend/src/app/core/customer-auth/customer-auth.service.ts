import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CustomerAuthResponse, CustomerLoginRequest, CustomerRegisterRequest, CustomerSession } from './customer-auth.models';

const KEY = 'acr.customer.session';

@Injectable({ providedIn: 'root' })
export class CustomerAuthService {
  private readonly http = inject(HttpClient);
  private readonly sessionSignal = signal<CustomerSession | null>(this.load());
  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.valid(this.sessionSignal()));

  login(request: CustomerLoginRequest): Observable<CustomerAuthResponse> {
    return this.http.post<CustomerAuthResponse>(`${environment.apiUrl}/api/customer/auth/login`, request)
      .pipe(tap((response) => this.store(response)));
  }
  register(request: CustomerRegisterRequest): Observable<CustomerAuthResponse> {
    return this.http.post<CustomerAuthResponse>(`${environment.apiUrl}/api/customer/auth/register`, request)
      .pipe(tap((response) => this.store(response)));
  }
  logout(): void { localStorage.removeItem(KEY); this.sessionSignal.set(null); }
  valid(session: CustomerSession | null): boolean {
    if (!session?.token) return false;
    try {
      const part = session.token.split('.')[1];
      const normalized = part.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, '=')));
      return typeof payload.exp === 'number' && payload.exp * 1000 > Date.now();
    } catch { return false; }
  }
  private store(response: CustomerAuthResponse): void { localStorage.setItem(KEY, JSON.stringify(response)); this.sessionSignal.set(response); }
  private load(): CustomerSession | null {
    try { const raw = localStorage.getItem(KEY); const value = raw ? JSON.parse(raw) as CustomerSession : null; if (this.valid(value)) return value; } catch {}
    localStorage.removeItem(KEY); return null;
  }
}
