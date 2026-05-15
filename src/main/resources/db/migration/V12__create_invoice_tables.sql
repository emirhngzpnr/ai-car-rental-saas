CREATE TABLE rental.invoice_counters (
                                         id BIGSERIAL PRIMARY KEY,

                                         tenant_id BIGINT NOT NULL,
                                         invoice_year INTEGER NOT NULL,
                                         last_number BIGINT NOT NULL DEFAULT 0,
                                         updated_at TIMESTAMP NOT NULL,

                                         CONSTRAINT fk_invoice_counters_tenant
                                             FOREIGN KEY (tenant_id)
                                                 REFERENCES rental.tenants(id),

                                         CONSTRAINT uk_invoice_counters_tenant_year
                                             UNIQUE (tenant_id, invoice_year),

                                         CONSTRAINT invoice_counters_last_number_check
                                             CHECK (last_number >= 0)
);

CREATE TABLE rental.invoices (
                                 id BIGSERIAL PRIMARY KEY,

                                 invoice_number VARCHAR(50) NOT NULL,

                                 tenant_id BIGINT NOT NULL,
                                 reservation_id BIGINT,
                                 rental_id BIGINT,

                                 type VARCHAR(50) NOT NULL,
                                 status VARCHAR(50) NOT NULL,

                                 customer_full_name_snapshot VARCHAR(150) NOT NULL,
                                 customer_email_snapshot VARCHAR(150) NOT NULL,
                                 customer_phone_snapshot VARCHAR(50),
                                 customer_identity_number_snapshot VARCHAR(50),

                                 vehicle_plate_number_snapshot VARCHAR(50) NOT NULL,
                                 vehicle_brand_snapshot VARCHAR(100),
                                 vehicle_model_snapshot VARCHAR(100),

                                 rental_amount NUMERIC(12, 2) NOT NULL,
                                 extra_km_amount NUMERIC(12, 2) NOT NULL,
                                 deposit_amount NUMERIC(12, 2) NOT NULL,
                                 deposit_deduction_amount NUMERIC(12, 2) NOT NULL,
                                 refund_amount NUMERIC(12, 2) NOT NULL,
                                 total_amount NUMERIC(12, 2) NOT NULL,

                                 currency VARCHAR(3) NOT NULL,
                                 issued_at TIMESTAMP NOT NULL,

                                 CONSTRAINT fk_invoices_tenant
                                     FOREIGN KEY (tenant_id)
                                         REFERENCES rental.tenants(id),

                                 CONSTRAINT fk_invoices_reservation
                                     FOREIGN KEY (reservation_id)
                                         REFERENCES rental.reservations(id),

                                 CONSTRAINT fk_invoices_rental
                                     FOREIGN KEY (rental_id)
                                         REFERENCES rental.rentals(id),

                                 CONSTRAINT uk_invoices_invoice_number
                                     UNIQUE (invoice_number),

                                 CONSTRAINT invoices_type_check
                                     CHECK (
                                         type IN (
                                                  'RENTAL_COMPLETION',
                                                  'REFUND'
                                             )
                                         ),

                                 CONSTRAINT invoices_status_check
                                     CHECK (
                                         status IN (
                                                    'ISSUED',
                                                    'CANCELLED'
                                             )
                                         ),

                                 CONSTRAINT invoices_amounts_check
                                     CHECK (
                                         rental_amount >= 0
                                             AND extra_km_amount >= 0
                                             AND deposit_amount >= 0
                                             AND deposit_deduction_amount >= 0
                                             AND refund_amount >= 0
                                             AND total_amount >= 0
                                         )
);

CREATE INDEX idx_invoice_counters_tenant_id
    ON rental.invoice_counters(tenant_id);

CREATE INDEX idx_invoices_tenant_id
    ON rental.invoices(tenant_id);

CREATE INDEX idx_invoices_invoice_number
    ON rental.invoices(invoice_number);

CREATE INDEX idx_invoices_status
    ON rental.invoices(status);

CREATE INDEX idx_invoices_issued_at
    ON rental.invoices(issued_at);