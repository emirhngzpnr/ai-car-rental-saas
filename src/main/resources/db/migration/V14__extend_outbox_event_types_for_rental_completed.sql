ALTER TABLE rental.outbox_messages
    DROP CONSTRAINT IF EXISTS outbox_messages_event_type_check;

ALTER TABLE rental.outbox_messages
    ADD CONSTRAINT outbox_messages_event_type_check
        CHECK (
            event_type IN (
                           'PAYMENT_COMPLETED',
                           'RESERVATION_CONFIRMED',
                           'RESERVATION_EXPIRED',
                           'RENTAL_COMPLETED'
                )
            );