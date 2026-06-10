export type AiPricingStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED';

export interface AiPricingRecommendationResponse {
  vehicleId: number;
  currentDailyPrice: number;
  recommendedDailyPrice: number;
  confidenceLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  reason: string;
}

export interface AiPricingRecommendationManagementResponse {
  id: number;
  tenantId: number;
  vehicleId: number;
  plateNumber: string;
  brand: string;
  model: string;
  currentPrice: number;
  recommendedPrice: number;
  confidenceLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  reason: string;
  status: AiPricingStatus;
  approvedByUserId: number | null;
  rejectedByUserId: number | null;
  approvedAt: string | null;
  rejectedAt: string | null;
  createdAt: string;
}
