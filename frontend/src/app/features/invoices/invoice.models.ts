export type InvoiceType = 'RENTAL_COMPLETION' | 'REFUND';
export type InvoiceStatus = 'ISSUED' | 'CANCELLED';

export interface InvoiceResponse {
  id: number;
  invoiceNumber: string;
  tenantId: number;
  reservationId: number;
  rentalId: number;
  type: InvoiceType;
  status: InvoiceStatus;
  customerFullNameSnapshot: string;
  customerEmailSnapshot: string;
  vehiclePlateNumberSnapshot: string;
  rentalAmount: number;
  extraKmAmount: number;
  depositAmount: number;
  depositDeductionAmount: number;
  refundAmount: number;
  totalAmount: number;
  currency: string;
  issuedAt: string;
}

export interface InvoiceQuery {
  status?: InvoiceStatus | '';
  type?: InvoiceType | '';
  page: number;
  size: number;
}
