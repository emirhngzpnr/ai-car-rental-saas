export interface VehicleSearchCriteria {
  pickupDateTime: string;
  returnDateTime: string;
  minDailyPrice?: number | null;
  maxDailyPrice?: number | null;
  minDailyKmLimit?: number | null;
  brand?: string;
  model?: string;
  category?: string;
  transmission?: string;
  fuelType?: string;
  minSeats?: number | null;
  location?: string;
  sort?: string;
  page?: number;
  size?: number;
}
export interface MarketplaceVehicle {
  vehicleId: number; tenantSlug: string; tenantName: string; brand: string; model: string;
  productionYear: number; dailyPrice: number; dailyKmLimit: number; extraKmPricePerKm: number;
  category: string | null; transmission: string | null; fuelType: string | null; seatCount: number | null;
  location: string | null; imageUrl: string | null;
}
export interface InsurancePackage { id: number; type: string; name: string; coverageDescription: string; dailyPrice: number; }
export interface MarketplaceVehicleDetail extends MarketplaceVehicle {
  tenantEmail: string; tenantPhone: string; insurancePackages: InsurancePackage[];
}
export interface MarketplaceSearchResponse { content: MarketplaceVehicle[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface ReservationResponse {
  reservationCode: string; status: string; customerEmail: string; vehicleBrand: string; vehicleModel: string;
  maskedPlateNumber: string; pickupDateTime: string; returnDateTime: string; depositAmount: number;
  estimatedRentalPrice: number; insuranceTotalPrice: number | null; totalEstimatedPrice: number;
}
export interface TrackingResponse {
  reservationCode: string; status: string; vehicleBrand: string; vehicleModel: string; maskedPlateNumber: string;
  pickupDateTime: string; returnDateTime: string; depositAmount: number; totalEstimatedPrice: number; paymentStatusSummary: string;
}
export interface CustomerReservation {
  reservationCode: string; status: string; tenantSlug: string; tenantName: string; vehicleId: number;
  vehicleBrand: string; vehicleModel: string; pickupDateTime: string; returnDateTime: string;
  depositAmount: number; estimatedRentalPrice: number; insuranceTotalPrice: number | null;
  totalEstimatedPrice: number; paymentStatusSummary: string;
}
