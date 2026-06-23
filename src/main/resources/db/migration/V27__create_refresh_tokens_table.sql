CREATE TABLE rental.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    principal_type VARCHAR(20) NOT NULL,
    principal_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT refresh_tokens_principal_type_check
        CHECK (principal_type IN ('STAFF', 'CUSTOMER'))
);

CREATE INDEX idx_refresh_tokens_principal
    ON rental.refresh_tokens(principal_type, principal_id);

CREATE INDEX idx_refresh_tokens_expires_at
    ON rental.refresh_tokens(expires_at);
