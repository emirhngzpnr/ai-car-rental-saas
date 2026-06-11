export type InsurancePackageType = 'BASIC' | 'STANDARD' | 'PREMIUM' | 'FULL_COVERAGE';

export interface InsurancePackageResponse {
  id: number;
  tenantId: number;
  tenantName: string;
  type: InsurancePackageType;
  name: string;
  coverageDescription: string;
  dailyPrice: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateInsurancePackageRequest {
  tenantId: number;
  type: InsurancePackageType;
  name: string;
  coverageDescription: string;
  dailyPrice: number;
}

export interface UpdateInsurancePackageRequest {
  type: InsurancePackageType;
  name: string;
  coverageDescription: string;
  dailyPrice: number;
  active: boolean;
}
