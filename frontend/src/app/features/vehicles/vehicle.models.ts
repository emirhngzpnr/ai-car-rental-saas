export type VehicleStatus = 'AVAILABLE' | 'RENTED' | 'MAINTENANCE' | 'PASSIVE';
export type VehicleCategory = 'ECONOMY' | 'COMPACT' | 'SEDAN' | 'SUV' | 'LUXURY' | 'VAN';
export type TransmissionType = 'MANUAL' | 'AUTOMATIC';
export type FuelType = 'GASOLINE' | 'DIESEL' | 'HYBRID' | 'ELECTRIC' | 'LPG';

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
  category: VehicleCategory | null;
  transmission: TransmissionType | null;
  fuelType: FuelType | null;
  seatCount: number | null;
  location: string | null;
  imageUrl: string | null;
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
  category: VehicleCategory | null;
  transmission: TransmissionType | null;
  fuelType: FuelType | null;
  seatCount: number | null;
  location: string | null;
  imageUrl: string | null;
}

export interface UpdateVehicleRequest extends CreateVehicleRequest {
  active: boolean;
}
