CREATE TABLE rental.tenant_settings (
                                        id BIGSERIAL PRIMARY KEY,

                                        tenant_id BIGINT NOT NULL,

                                        setting_key VARCHAR(100) NOT NULL,
                                        setting_value VARCHAR(500) NOT NULL,

                                        description TEXT,

                                        active BOOLEAN NOT NULL DEFAULT TRUE,

                                        created_at TIMESTAMP NOT NULL,
                                        updated_at TIMESTAMP NOT NULL,

                                        CONSTRAINT fk_tenant_settings_tenant
                                            FOREIGN KEY (tenant_id)
                                                REFERENCES rental.tenants(id),

                                        CONSTRAINT uk_tenant_settings_tenant_key
                                            UNIQUE (tenant_id, setting_key)
);

CREATE INDEX idx_tenant_settings_tenant_id
    ON rental.tenant_settings(tenant_id);

CREATE INDEX idx_tenant_settings_setting_key
    ON rental.tenant_settings(setting_key);