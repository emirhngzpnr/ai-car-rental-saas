ALTER TABLE rental.notifications
    DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE rental.notifications
    ADD CONSTRAINT notifications_type_check
        CHECK (
            type IN (
                     'PAYMENT_COMPLETED',
                     'RESERVATION_CONFIRMED',
                     'RESERVATION_EXPIRED',
                     'RENTAL_STARTED',
                     'RENTAL_COMPLETED',
                     'REFUND_PROCESSED',
                     'REFUND_COMPLETED'
                )
            );