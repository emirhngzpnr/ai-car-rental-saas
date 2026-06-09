export interface ReservationResponse {
  id: number;
  tenantId: number;
  tenantName: string;
  vehicleId: number;
  vehicleBrand: string;
  vehicleModel: string;
  vehiclePlateNumber: string;
  customerFullName: string;
  customerPhone: string;
  customerEmail: string;
  customerIdentityNumber: string;
  pickupDateTime: string;
  returnDateTime: string;
  dailyPriceSnapshot: number;
  dailyKmLimitSnapshot: number;
  extraKmPricePerKmSnapshot: number;
  insurancePackageId: number | null;
  insurancePackageNameSnapshot: string | null;
  insurancePackageTypeSnapshot: string | null;
  insuranceDailyPriceSnapshot: number | null;
  insuranceTotalPriceSnapshot: number | null;
  depositAmount: number;
  estimatedRentalPrice: number;
  totalEstimatedPrice: number;
  status: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateReservationRequest {
  vehicleId: number;
  customerFullName: string;
  customerPhone: string;
  customerEmail: string;
  customerIdentityNumber: string;
  pickupDateTime: string;
  insurancePackageId: number | null;
  returnDateTime: string;
}
