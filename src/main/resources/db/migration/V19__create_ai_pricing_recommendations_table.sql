CREATE TABLE rental.ai_pricing_recommendations (
                                                   id BIGSERIAL PRIMARY KEY,

                                                   tenant_id BIGINT NOT NULL,
                                                   vehicle_id BIGINT NOT NULL,

                                                   current_price NUMERIC(12, 2) NOT NULL,
                                                   recommended_price NUMERIC(12, 2) NOT NULL,

                                                   confidence_level VARCHAR(20) NOT NULL,
                                                   reason TEXT,

                                                   status VARCHAR(30) NOT NULL,

                                                   approved_by_user_id BIGINT,
                                                   rejected_by_user_id BIGINT,

                                                   approved_at TIMESTAMP,
                                                   rejected_at TIMESTAMP,

                                                   created_at TIMESTAMP NOT NULL,
                                                   updated_at TIMESTAMP NOT NULL,

                                                   CONSTRAINT fk_ai_pricing_tenant
                                                       FOREIGN KEY (tenant_id)
                                                           REFERENCES rental.tenants(id),

                                                   CONSTRAINT fk_ai_pricing_vehicle
                                                       FOREIGN KEY (vehicle_id)
                                                           REFERENCES rental.vehicles(id),

                                                   CONSTRAINT fk_ai_pricing_approved_by_user
                                                       FOREIGN KEY (approved_by_user_id)
                                                           REFERENCES rental.users(id),

                                                   CONSTRAINT fk_ai_pricing_rejected_by_user
                                                       FOREIGN KEY (rejected_by_user_id)
                                                           REFERENCES rental.users(id),

                                                   CONSTRAINT ai_pricing_status_check
                                                       CHECK (
                                                           status IN (
                                                                      'PENDING',
                                                                      'APPROVED',
                                                                      'REJECTED',
                                                                      'EXPIRED'
                                                               )
                                                           )
);

CREATE INDEX idx_ai_pricing_tenant_id
    ON rental.ai_pricing_recommendations(tenant_id);

CREATE INDEX idx_ai_pricing_vehicle_id
    ON rental.ai_pricing_recommendations(vehicle_id);

CREATE INDEX idx_ai_pricing_status
    ON rental.ai_pricing_recommendations(status);