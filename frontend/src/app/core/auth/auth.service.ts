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

  private readonly sessionSignal = signal<AuthSession | null>(
    this.authStorage.getSession()
  );

  readonly session = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => Boolean(this.sessionSignal()?.token));
  readonly currentRole = computed(() => this.sessionSignal()?.role ?? null);

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

  hasAnyRole(roles: UserRole[]): boolean {
    const role = this.currentRole();
    return Boolean(role && roles.includes(role));
  }
}
