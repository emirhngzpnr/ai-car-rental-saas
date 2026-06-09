export interface RentalResponse {
  id: number;
  reservationId: number;
  tenantId: number;
  tenantName: string;
  vehicleId: number;
  vehicleBrand: string;
  vehicleModel: string;
  vehiclePlateNumber: string;
  actualPickupDateTime: string;
  actualReturnDateTime: string | null;
  startMileage: number;
  endMileage: number | null;
  usedKm: number | null;
  allowedKm: number;
  extraKm: number | null;
  extraKmFee: number | null;
  finalRentalPrice: number | null;
  depositDeduction: number | null;
  refundAmount: number | null;
  status: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface StartRentalRequest {
  reservationId: number;
  actualPickupDateTime: string;
  startMileage: number;
}

export interface CompleteRentalRequest {
  actualReturnDateTime: string;
  endMileage: number;
}
