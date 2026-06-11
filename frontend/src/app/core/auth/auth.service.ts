import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, AuthSession, LoginRequest, UserRole } from './auth.models';
import { AuthStorageService } from './auth-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly authStorage = inject(AuthStorageService);

  private readonly sessionSignal = signal<AuthSession | null>(this.loadInitialSession());

  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.isSessionValid(this.sessionSignal()));
  readonly currentRole = computed(() => {
    const session = this.sessionSignal();
    return this.isSessionValid(session) ? session?.role ?? null : null;
  });

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/api/auth/login`, request)
      .pipe(
        tap((response) => {
          const session: AuthSession = {
            token: response.token,
            tokenType: response.tokenType,
            userId: response.userId,
            email: response.email,
            role: response.role,
            tenantId: response.tenantId
          };

          this.authStorage.setSession(session);
          this.sessionSignal.set(session);
        })
      );
  }

  logout(): void {
    this.authStorage.clearSession();
    this.sessionSignal.set(null);
  }

  isSessionValid(session: AuthSession | null): boolean {
    return Boolean(session?.token && !this.isTokenExpired(session.token));
  }

  hasAnyRole(roles: UserRole[]): boolean {
    const role = this.currentRole();
    return Boolean(role && roles.includes(role));
  }

  private loadInitialSession(): AuthSession | null {
    const session = this.authStorage.getSession();

    if (!this.isSessionValid(session)) {
      this.authStorage.clearSession();
      return null;
    }

    return session;
  }

  private isTokenExpired(token: string): boolean {
    const payload = this.decodeJwtPayload(token);
    if (!payload?.exp) return true;

    const expiresAt = payload.exp * 1000;
    return expiresAt <= Date.now();
  }

  private decodeJwtPayload(token: string): { exp?: number } | null {
    try {
      const payload = token.split('.')[1];
      if (!payload) return null;

      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(normalized.length + (4 - normalized.length % 4) % 4, '=');
      return JSON.parse(atob(padded)) as { exp?: number };
    } catch {
      return null;
    }
  }
}
