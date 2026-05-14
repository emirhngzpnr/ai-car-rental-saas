CREATE TABLE rental.outbox_messages (
                                        id BIGSERIAL PRIMARY KEY,

                                        topic VARCHAR(100) NOT NULL,
                                        message_key VARCHAR(100) NOT NULL,

                                        event_type VARCHAR(100) NOT NULL,
                                        payload JSONB NOT NULL,

                                        status VARCHAR(50) NOT NULL,
                                        retry_count INTEGER NOT NULL DEFAULT 0,
                                        error_message TEXT,

                                        created_at TIMESTAMP NOT NULL,
                                        processed_at TIMESTAMP,

                                        CONSTRAINT outbox_messages_event_type_check
                                            CHECK (
                                                event_type IN (
                                                               'PAYMENT_COMPLETED',
                                                               'RESERVATION_CONFIRMED',
                                                               'RESERVATION_EXPIRED'
                                                    )
                                                ),

                                        CONSTRAINT outbox_messages_status_check
                                            CHECK (
                                                status IN (
                                                           'PENDING',
                                                           'PUBLISHED',
                                                           'FAILED'
                                                    )
                                                ),

                                        CONSTRAINT outbox_messages_retry_count_check
                                            CHECK (retry_count >= 0)
);

CREATE INDEX idx_outbox_messages_status
    ON rental.outbox_messages(status);

CREATE INDEX idx_outbox_messages_event_type
    ON rental.outbox_messages(event_type);

CREATE INDEX idx_outbox_messages_created_at
    ON rental.outbox_messages(created_at);