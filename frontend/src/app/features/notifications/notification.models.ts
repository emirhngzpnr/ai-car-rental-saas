export type NotificationType =
  | 'RESERVATION_CREATED'
  | 'RESERVATION_CONFIRMED'
  | 'RESERVATION_EXPIRED'
  | 'PAYMENT_COMPLETED'
  | 'RENTAL_STARTED'
  | 'RENTAL_COMPLETED'
  | 'REFUND_PROCESSED'
  | 'REFUND_COMPLETED'
  | 'AI_PRICING_APPROVED';

export type NotificationChannel = 'EMAIL' | 'SMS' | 'PUSH';
export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED';

export interface NotificationResponse {
  id: number;
  tenantId: number;
  type: NotificationType;
  channel: NotificationChannel;
  status: NotificationStatus;
  recipient: string;
  subject: string;
  message: string;
  errorMessage: string | null;
  sentAt: string | null;
  createdAt: string;
}
