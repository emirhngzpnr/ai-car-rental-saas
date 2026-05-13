CREATE TABLE rental.insurance_packages (
                                           id BIGSERIAL PRIMARY KEY,

                                           tenant_id BIGINT NOT NULL,

                                           type VARCHAR(50) NOT NULL,
                                           name VARCHAR(100) NOT NULL,
                                           coverage_description VARCHAR(1000) NOT NULL,
                                           daily_price NUMERIC(12, 2) NOT NULL,

                                           active BOOLEAN NOT NULL DEFAULT TRUE,

                                           created_at TIMESTAMP NOT NULL,
                                           updated_at TIMESTAMP,

                                           CONSTRAINT fk_insurance_packages_tenant
                                               FOREIGN KEY (tenant_id)
                                                   REFERENCES rental.tenants(id),

                                           CONSTRAINT insurance_packages_type_check
                                               CHECK (
                                                   type IN (
                                                            'BASIC',
                                                            'STANDARD',
                                                            'PREMIUM',
                                                            'FULL_COVERAGE'
                                                       )
                                                   ),

                                           CONSTRAINT insurance_packages_daily_price_check
                                               CHECK (daily_price >= 0)
);

CREATE INDEX idx_insurance_packages_tenant_id
    ON rental.insurance_packages(tenant_id);

CREATE INDEX idx_insurance_packages_type
    ON rental.insurance_packages(type);