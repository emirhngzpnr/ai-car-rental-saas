CREATE TABLE rental.vehicle_reviews (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    customer_account_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    rating INTEGER NOT NULL,
    title VARCHAR(100),
    comment VARCHAR(1000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_vehicle_reviews_tenant
        FOREIGN KEY (tenant_id) REFERENCES rental.tenants(id),
    CONSTRAINT fk_vehicle_reviews_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES rental.vehicles(id),
    CONSTRAINT fk_vehicle_reviews_customer_account
        FOREIGN KEY (customer_account_id) REFERENCES rental.customer_accounts(id),
    CONSTRAINT fk_vehicle_reviews_reservation
        FOREIGN KEY (reservation_id) REFERENCES rental.reservations(id),
    CONSTRAINT uq_vehicle_reviews_reservation UNIQUE (reservation_id),
    CONSTRAINT chk_vehicle_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_vehicle_reviews_comment_not_blank CHECK (length(trim(comment)) > 0)
);

CREATE INDEX idx_vehicle_reviews_tenant_active
    ON rental.vehicle_reviews(tenant_id, active);

CREATE INDEX idx_vehicle_reviews_vehicle_active
    ON rental.vehicle_reviews(vehicle_id, active);

CREATE INDEX idx_vehicle_reviews_rating
    ON rental.vehicle_reviews(rating);
