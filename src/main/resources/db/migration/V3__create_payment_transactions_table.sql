CREATE TABLE rental.payment_transactions (
                                             id BIGSERIAL PRIMARY KEY,

                                             tenant_id BIGINT NOT NULL,
                                             reservation_id BIGINT NULL,
                                             rental_id BIGINT NULL,

                                             payment_type VARCHAR(50) NOT NULL,
                                             payment_status VARCHAR(50) NOT NULL,

                                             amount NUMERIC(12, 2) NOT NULL,
                                             currency VARCHAR(3) NOT NULL,

                                             provider_transaction_id VARCHAR(100),
                                             idempotency_key VARCHAR(100) NOT NULL,

                                             created_at TIMESTAMP NOT NULL,
                                             updated_at TIMESTAMP,

                                             CONSTRAINT fk_payment_transactions_tenant
                                                 FOREIGN KEY (tenant_id)
                                                     REFERENCES rental.tenants(id),

                                             CONSTRAINT fk_payment_transactions_reservation
                                                 FOREIGN KEY (reservation_id)
                                                     REFERENCES rental.reservations(id),

                                             CONSTRAINT fk_payment_transactions_rental
                                                 FOREIGN KEY (rental_id)
                                                     REFERENCES rental.rentals(id),

                                             CONSTRAINT uk_payment_transactions_idempotency_key
                                                 UNIQUE (idempotency_key),

                                             CONSTRAINT payment_transactions_type_check
                                                 CHECK (
                                                     payment_type IN (
                                                                      'DEPOSIT_PAYMENT',
                                                                      'RENTAL_PAYMENT',
                                                                      'REFUND',
                                                                      'EXTRA_KM_CHARGE'
                                                         )
                                                     ),

                                             CONSTRAINT payment_transactions_status_check
                                                 CHECK (
                                                     payment_status IN (
                                                                        'PENDING',
                                                                        'SUCCESS',
                                                                        'FAILED',
                                                                        'REFUNDED'
                                                         )
                                                     ),

                                             CONSTRAINT payment_transactions_amount_check
                                                 CHECK (amount >= 0)
);

CREATE INDEX idx_payment_transactions_tenant_id
    ON rental.payment_transactions(tenant_id);

CREATE INDEX idx_payment_transactions_reservation_id
    ON rental.payment_transactions(reservation_id);

CREATE INDEX idx_payment_transactions_rental_id
    ON rental.payment_transactions(rental_id);

CREATE INDEX idx_payment_transactions_idempotency_key
    ON rental.payment_transactions(idempotency_key);