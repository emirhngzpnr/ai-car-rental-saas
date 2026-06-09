export type VehicleStatus = 'AVAILABLE' | 'RENTED' | 'MAINTENANCE' | 'PASSIVE';

export interface VehicleResponse {
  id: number;
  brand: string;
  model: string;
  plateNumber: string;
  productionYear: number;
  currentMileage: number;
  dailyPrice: number;
  dailyKmLimit: number;
  extraKmPricePerKm: number;
  status: VehicleStatus;
  active: boolean;
  tenantId: number | null;
  tenantName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateVehicleRequest {
  brand: string;
  model: string;
  plateNumber: string;
  productionYear: number;
  currentMileage: number;
  dailyPrice: number;
  dailyKmLimit: number;
  extraKmPricePerKm: number;
  status: VehicleStatus;
}

export interface UpdateVehicleRequest extends CreateVehicleRequest {
  active: boolean;
}
