ALTER TABLE rental.customer_accounts
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified_at TIMESTAMP,
    ADD COLUMN last_verification_email_sent_at TIMESTAMP;

CREATE TABLE rental.customer_account_tokens (
    id BIGSERIAL PRIMARY KEY,
    customer_account_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    type VARCHAR(40) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    request_ip VARCHAR(64),

    CONSTRAINT fk_customer_account_tokens_customer
        FOREIGN KEY (customer_account_id)
        REFERENCES rental.customer_accounts(id)
);

CREATE UNIQUE INDEX uk_customer_account_tokens_token_hash
    ON rental.customer_account_tokens(token_hash);

CREATE INDEX idx_customer_account_tokens_customer_type
    ON rental.customer_account_tokens(customer_account_id, type, used_at, expires_at);
