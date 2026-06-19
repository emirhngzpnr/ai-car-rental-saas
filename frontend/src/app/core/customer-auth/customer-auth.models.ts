export interface CustomerAuthResponse {
  token: string;
  tokenType: string;
  customerId: number;
  email: string;
  firstName: string;
  lastName: string;
}

export interface CustomerSession extends CustomerAuthResponse {}
export interface CustomerRegisterRequest { firstName: string; lastName: string; email: string; password: string; phone: string; }
export interface CustomerLoginRequest { email: string; password: string; }
export interface CustomerProfile { id: number; firstName: string; lastName: string; email: string; phone: string; }
