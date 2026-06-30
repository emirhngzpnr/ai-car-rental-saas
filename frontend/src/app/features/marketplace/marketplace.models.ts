export interface VehicleSearchCriteria {
  pickupDateTime: string;
  returnDateTime: string;
  minDailyPrice?: number | null;
  maxDailyPrice?: number | null;
  minDailyKmLimit?: number | null;
  brand?: string;
  model?: string;
  category?: string;
  categories?: string[];
  transmission?: string;
  fuelType?: string;
  minSeats?: number | null;
  location?: string;
  sort?: string;
  page?: number;
  size?: number;
}
export interface SemanticVehicleSearchRequest {
  query: string;
  pickupDateTime?: string;
  returnDateTime?: string;
  location?: string;
}
export interface SemanticVehicleSearchCriteria {
  minDailyPrice: number | null;
  maxDailyPrice: number | null;
  minDailyKmLimit: number | null;
  brand: string | null;
  model: string | null;
  categories: string[];
  transmission: string | null;
  fuelType: string | null;
  minSeats: number | null;
  location: string | null;
  sort: string | null;
}
export interface SemanticVehicleSearchResponse {
  criteria: SemanticVehicleSearchCriteria;
  dateCriteria: {
    pickupDateTime: string | null;
    returnDateTime: string | null;
  };
  interpretation: {
    priceIntent: string | null;
    segmentIntent: string | null;
    dateIntent: string | null;
  };
  missingFields: string[];
  summary: string;
  inferences: string[];
  warnings: string[];
}
export interface MarketplaceVehicle {
  vehicleId: number; tenantSlug: string; tenantName: string; brand: string; model: string;
  productionYear: number; dailyPrice: number; dailyKmLimit: number; extraKmPricePerKm: number;
  category: string | null; transmission: string | null; fuelType: string | null; seatCount: number | null;
  location: string | null; imageUrl: string | null; averageRating: number; reviewCount: number;
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
export interface VehicleReview {
  id: number; rating: number; title: string | null; comment: string; customerDisplayName: string; createdAt: string;
}
export interface VehicleReviewPage {
  content: VehicleReview[]; page: number; size: number; totalElements: number; totalPages: number;
}
export interface CustomerVehicleReview {
  id: number; reservationCode: string; vehicleId: number; vehicleBrand: string; vehicleModel: string;
  rating: number; title: string | null; comment: string; createdAt: string; updatedAt: string;
}
export interface VehicleReviewRequest { rating: number; title?: string | null; comment: string; }
