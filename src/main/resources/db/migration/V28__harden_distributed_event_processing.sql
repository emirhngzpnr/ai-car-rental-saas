ALTER TABLE rental.outbox_messages
    ADD COLUMN next_attempt_at TIMESTAMP,
    ADD COLUMN last_attempt_at TIMESTAMP;

CREATE INDEX idx_outbox_messages_pending_retry
    ON rental.outbox_messages(status, next_attempt_at, created_at);

CREATE TABLE rental.processed_kafka_events (
    id BIGSERIAL PRIMARY KEY,
    consumer_name VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    message_key VARCHAR(200) NOT NULL,
    processed_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_processed_kafka_event
        UNIQUE (consumer_name, topic, message_key)
);

CREATE INDEX idx_processed_kafka_events_processed_at
    ON rental.processed_kafka_events(processed_at);

CREATE UNIQUE INDEX uk_invoices_rental_id
    ON rental.invoices(rental_id)
    WHERE rental_id IS NOT NULL;
