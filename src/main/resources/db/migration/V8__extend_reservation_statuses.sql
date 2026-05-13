ALTER TABLE rental.reservations
    DROP CONSTRAINT IF EXISTS reservations_status_check;

ALTER TABLE rental.reservations
    ADD CONSTRAINT reservations_status_check
        CHECK (
            status IN (
                       'PENDING_PAYMENT',
                       'DEPOSIT_PAID',
                       'CONFIRMED',
                       'CONVERTED_TO_RENTAL',
                       'COMPLETED',
                       'CANCELLED',
                       'EXPIRED'
                )
            );