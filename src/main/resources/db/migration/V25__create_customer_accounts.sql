CREATE TABLE rental.customer_accounts (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_customer_accounts_email_lower
    ON rental.customer_accounts (LOWER(email));

ALTER TABLE rental.reservations
    ADD COLUMN IF NOT EXISTS customer_account_id BIGINT;

ALTER TABLE rental.reservations
    ADD CONSTRAINT fk_reservations_customer_account
        FOREIGN KEY (customer_account_id)
        REFERENCES rental.customer_accounts(id);

CREATE INDEX idx_reservations_customer_account
    ON rental.reservations(customer_account_id);

