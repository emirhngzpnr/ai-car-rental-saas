ALTER TABLE rental.vehicles
    ADD COLUMN IF NOT EXISTS category VARCHAR(40),
    ADD COLUMN IF NOT EXISTS transmission VARCHAR(40),
    ADD COLUMN IF NOT EXISTS fuel_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS seat_count INTEGER,
    ADD COLUMN IF NOT EXISTS location VARCHAR(150),
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(1000);

ALTER TABLE rental.vehicles
    ADD CONSTRAINT chk_vehicles_seat_count
        CHECK (seat_count IS NULL OR seat_count > 0);

CREATE INDEX IF NOT EXISTS idx_vehicles_public_catalog
    ON rental.vehicles(active, status, daily_price, daily_km_limit);

