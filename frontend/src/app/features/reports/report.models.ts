export interface MonthlyRevenueResponse {
  month: string;
  totalRevenue: number;
}

export interface MonthlySummaryResponse {
  year: number;
  month: number;
  totalRevenue: number;
  completedRentals: number;
  refundAmount: number;
  extraKmRevenue: number;
  issuedInvoices: number;
}

export interface TopVehicleResponse {
  vehicleId: number;
  plateNumber: string;
  brand: string;
  model: string;
  rentalCount: number;
  totalRevenue: number;
}
