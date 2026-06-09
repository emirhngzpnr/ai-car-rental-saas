export type PaymentType = 'DEPOSIT_PAYMENT' | 'RENTAL_PAYMENT' | 'REFUND' | 'EXTRA_KM_CHARGE';
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED';

export interface CreatePaymentRequest {
  tenantId: number;
  reservationId: number | null;
  rentalId: number | null;
  paymentType: PaymentType;
  amount: number;
  idempotencyKey: string;
}

export interface PaymentTransactionResponse {
  id: number;
  tenantId: number;
  reservationId: number | null;
  rentalId: number | null;
  paymentType: PaymentType;
  paymentStatus: PaymentStatus;
  amount: number;
  currency: string;
  providerTransactionId: string;
  idempotencyKey: string;
  createdAt: string;
  updatedAt: string;
}
