ALTER TABLE rental.reservations
    ADD COLUMN insurance_package_id BIGINT,
    ADD COLUMN insurance_package_name_snapshot VARCHAR(100),
    ADD COLUMN insurance_package_type_snapshot VARCHAR(50),
    ADD COLUMN insurance_daily_price_snapshot NUMERIC(12, 2),
    ADD COLUMN insurance_total_price_snapshot NUMERIC(12, 2);

ALTER TABLE rental.reservations
    ADD CONSTRAINT fk_reservations_insurance_package
        FOREIGN KEY (insurance_package_id)
            REFERENCES rental.insurance_packages(id);

ALTER TABLE rental.reservations
    ADD CONSTRAINT reservations_insurance_type_snapshot_check
        CHECK (
            insurance_package_type_snapshot IS NULL
                OR insurance_package_type_snapshot IN (
                                                       'BASIC',
                                                       'STANDARD',
                                                       'PREMIUM',
                                                       'FULL_COVERAGE'
                )
            );

ALTER TABLE rental.reservations
    ADD CONSTRAINT reservations_insurance_daily_price_check
        CHECK (
            insurance_daily_price_snapshot IS NULL
                OR insurance_daily_price_snapshot >= 0
            );

ALTER TABLE rental.reservations
    ADD CONSTRAINT reservations_insurance_total_price_check
        CHECK (
            insurance_total_price_snapshot IS NULL
                OR insurance_total_price_snapshot >= 0
            );

CREATE INDEX idx_reservations_insurance_package_id
    ON rental.reservations(insurance_package_id);