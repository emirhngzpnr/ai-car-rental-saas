export type UserRole = 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'TENANT_STAFF';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface AuthMessageResponse {
  message: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  userId: number;
  email: string;
  role: UserRole;
  tenantId: number | null;
}

export interface AuthSession {
  token: string;
  tokenType: string;
  userId: number;
  email: string;
  role: UserRole;
  tenantId: number | null;
}
