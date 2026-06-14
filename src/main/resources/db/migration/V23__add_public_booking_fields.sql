ALTER TABLE rental.tenants
    ADD COLUMN IF NOT EXISTS slug VARCHAR(255);

UPDATE rental.tenants
SET slug = COALESCE(
        NULLIF(
            regexp_replace(
                lower(coalesce(sub_domain, company_name, 'tenant-' || id)),
                '[^a-z0-9]+',
                '-',
                'g'
            ),
            ''
        ),
        'tenant-' || id
    )
WHERE slug IS NULL;

ALTER TABLE rental.tenants
    ALTER COLUMN slug SET NOT NULL;

ALTER TABLE rental.tenants
    ADD CONSTRAINT uk_tenants_slug UNIQUE (slug);

CREATE INDEX IF NOT EXISTS idx_tenants_slug
    ON rental.tenants(slug);

ALTER TABLE rental.reservations
    ADD COLUMN IF NOT EXISTS reservation_code VARCHAR(50);

UPDATE rental.reservations
SET reservation_code = 'RNT-' ||
                       EXTRACT(YEAR FROM COALESCE(created_at, now()))::int ||
                       '-' ||
                       lpad(id::text, 6, '0')
WHERE reservation_code IS NULL;

ALTER TABLE rental.reservations
    ADD CONSTRAINT uk_reservations_reservation_code UNIQUE (reservation_code);

CREATE INDEX IF NOT EXISTS idx_reservations_reservation_code
    ON rental.reservations(reservation_code);
