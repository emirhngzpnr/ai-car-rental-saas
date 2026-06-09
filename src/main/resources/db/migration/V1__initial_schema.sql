CREATE SCHEMA IF NOT EXISTS rental;

CREATE TABLE rental.tenants (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(255),
    sub_domain VARCHAR(255),
    active BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    phone_number VARCHAR(255),
    email VARCHAR(255)
);

CREATE TABLE rental.users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    email VARCHAR(255),
    password_hash VARCHAR(255),
    role VARCHAR(255),
    active BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    tenant_id BIGINT,

    CONSTRAINT fk_users_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES rental.tenants(id),

    CONSTRAINT users_role_check
        CHECK (
            role IS NULL OR role IN (
                'SUPER_ADMIN',
                'TENANT_ADMIN',
                'TENANT_STAFF'
            )
        )
);

CREATE TABLE rental.vehicles (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    plate_number VARCHAR(255) NOT NULL,
    production_year INTEGER,
    current_mileage INTEGER NOT NULL,
    daily_km_limit INTEGER NOT NULL,
    extra_km_price_per_km NUMERIC(38, 2) NOT NULL,
    daily_price NUMERIC(38, 2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    tenant_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_vehicles_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES rental.tenants(id),

    CONSTRAINT uk_vehicle_tenant_plate
        UNIQUE (tenant_id, plate_number),

    CONSTRAINT vehicles_status_check
        CHECK (
            status IN (
                'AVAILABLE',
                'RENTED',
                'MAINTENANCE',
                'PASSIVE'
            )
        )
);

CREATE TABLE rental.reservations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    customer_full_name VARCHAR(255) NOT NULL,
    customer_phone VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_identity_number VARCHAR(255) NOT NULL,
    pickup_date_time TIMESTAMP NOT NULL,
    return_date_time TIMESTAMP NOT NULL,
    daily_price_snapshot NUMERIC(38, 2) NOT NULL,
    daily_km_limit_snapshot INTEGER NOT NULL,
    extra_km_price_per_km_snapshot NUMERIC(38, 2) NOT NULL,
    deposit_amount NUMERIC(38, 2) NOT NULL,
    estimated_rental_price NUMERIC(38, 2) NOT NULL,
    total_estimated_price NUMERIC(38, 2) NOT NULL,
    status VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_reservations_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES rental.tenants(id),

    CONSTRAINT fk_reservations_vehicle
        FOREIGN KEY (vehicle_id)
            REFERENCES rental.vehicles(id),

    CONSTRAINT reservations_status_check
        CHECK (
            status IN (
                'PENDING_PAYMENT',
                'CONFIRMED',
                'CONVERTED_TO_RENTAL',
                'CANCELLED'
            )
        )
);

CREATE TABLE rental.rentals (
    id BIGSERIAL PRIMARY KEY,
    reservation_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    actual_pickup_date_time TIMESTAMP NOT NULL,
    actual_return_date_time TIMESTAMP,
    start_mileage INTEGER NOT NULL,
    end_mileage INTEGER,
    used_km INTEGER,
    allowed_km INTEGER,
    extra_km INTEGER,
    extra_km_fee NUMERIC(38, 2),
    final_rental_price NUMERIC(38, 2),
    deposit_deduction NUMERIC(38, 2),
    refund_amount NUMERIC(38, 2),
    status VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_rentals_reservation
        FOREIGN KEY (reservation_id)
            REFERENCES rental.reservations(id),

    CONSTRAINT fk_rentals_vehicle
        FOREIGN KEY (vehicle_id)
            REFERENCES rental.vehicles(id),

    CONSTRAINT fk_rentals_tenant
        FOREIGN KEY (tenant_id)
            REFERENCES rental.tenants(id),

    CONSTRAINT uk_rentals_reservation_id
        UNIQUE (reservation_id),

    CONSTRAINT rentals_status_check
        CHECK (
            status IN (
                'ACTIVE',
                'COMPLETED',
                'CANCELLED'
            )
        )
);

CREATE TABLE rental.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    actor_email VARCHAR(255),
    actor_role VARCHAR(255),
    tenant_id BIGINT,
    action VARCHAR(255) NOT NULL,
    target_type VARCHAR(255),
    target_id BIGINT,
    description TEXT,
    created_at TIMESTAMP
);

CREATE INDEX idx_users_tenant_id
    ON rental.users(tenant_id);

CREATE INDEX idx_vehicles_tenant_id
    ON rental.vehicles(tenant_id);

CREATE INDEX idx_reservations_tenant_id
    ON rental.reservations(tenant_id);

CREATE INDEX idx_reservations_vehicle_id
    ON rental.reservations(vehicle_id);

CREATE INDEX idx_reservations_status
    ON rental.reservations(status);

CREATE INDEX idx_rentals_tenant_id
    ON rental.rentals(tenant_id);

CREATE INDEX idx_rentals_vehicle_id
    ON rental.rentals(vehicle_id);

CREATE INDEX idx_audit_logs_tenant_id
    ON rental.audit_logs(tenant_id);

CREATE INDEX idx_audit_logs_action
    ON rental.audit_logs(action);
