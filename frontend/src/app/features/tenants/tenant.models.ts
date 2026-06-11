export interface TenantResponse {
  id: number;
  companyName: string;
  subDomain: string;
  active: boolean;
  email: string;
  phoneNumber: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTenantRequest {
  companyName: string;
  subDomain: string;
  email: string;
  phoneNumber: string;
}

export interface UpdateTenantRequest extends CreateTenantRequest {
  active: boolean;
}
