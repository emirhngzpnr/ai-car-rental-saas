export interface DashboardSummaryResponse {
  activeRentalsCount: number;
  completedRentalsCount: number;
  totalRevenue: number;
  totalRefundAmount: number;
  totalInvoiceAmount: number;
  totalExtraKmRevenue: number;
  pendingPaymentsCount: number;
  availableVehiclesCount: number;
}
