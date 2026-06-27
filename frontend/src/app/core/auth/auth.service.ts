import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, finalize, firstValueFrom, map, of, shareReplay, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthMessageResponse, AuthResponse, AuthSession, LoginRequest, SetPasswordRequest, UserRole } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly sessionSignal = signal<AuthSession | null>(null);
  private refreshRequest$: Observable<AuthSession> | null = null;

  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.isSessionValid(this.sessionSignal()));
  readonly currentRole = computed(() => {
    const session = this.sessionSignal();
    return this.isSessionValid(session) ? session?.role ?? null : null;
  });

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/api/auth/login`, request, { withCredentials: true })
      .pipe(
        tap((response) => {
          this.sessionSignal.set(this.toSession(response));
        })
      );
  }

  setPassword(request: SetPasswordRequest): Observable<AuthMessageResponse> {
    return this.http.post<AuthMessageResponse>(`${environment.apiUrl}/api/auth/set-password`, request, {
      withCredentials: true
    });
  }

  refresh(): Observable<AuthSession> {
    if (this.refreshRequest$) {
      return this.refreshRequest$;
    }

    this.refreshRequest$ = this.http
      .post<AuthResponse>(`${environment.apiUrl}/api/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        map((response) => this.toSession(response)),
        tap((session) => this.sessionSignal.set(session)),
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
    this.http
      .post<void>(`${environment.apiUrl}/api/auth/logout`, {}, { withCredentials: true })
      .pipe(catchError(() => of(undefined)))
      .subscribe();
  }

  isSessionValid(session: AuthSession | null): boolean {
    return Boolean(session?.token && !this.isTokenExpired(session.token));
  }

  hasAnyRole(roles: UserRole[]): boolean {
    const role = this.currentRole();
    return Boolean(role && roles.includes(role));
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

  private toSession(response: AuthResponse): AuthSession {
    return {
      token: response.token,
      tokenType: response.tokenType,
      userId: response.userId,
      email: response.email,
      role: response.role,
      tenantId: response.tenantId
    };
  }
}
