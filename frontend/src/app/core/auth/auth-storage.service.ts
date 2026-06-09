import { Injectable } from '@angular/core';
import { AuthSession } from './auth.models';

const SESSION_KEY = 'acr.session';

@Injectable({ providedIn: 'root' })
export class AuthStorageService {
  getSession(): AuthSession | null {
    const stored = localStorage.getItem(SESSION_KEY);

    if (!stored) {
      return null;
    }

    try {
      return JSON.parse(stored) as AuthSession;
    } catch {
      this.clearSession();
      return null;
    }
  }

  setSession(session: AuthSession): void {
    localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }

  clearSession(): void {
    localStorage.removeItem(SESSION_KEY);
  }
}
