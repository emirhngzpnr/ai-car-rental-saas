CREATE TABLE rental.user_invitation_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    invited_by_user_id BIGINT,

    CONSTRAINT fk_user_invitation_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES rental.users(id),

    CONSTRAINT fk_user_invitation_tokens_invited_by_user
        FOREIGN KEY (invited_by_user_id)
        REFERENCES rental.users(id)
);

CREATE UNIQUE INDEX uk_user_invitation_tokens_token_hash
    ON rental.user_invitation_tokens(token_hash);

CREATE INDEX idx_user_invitation_tokens_user
    ON rental.user_invitation_tokens(user_id, used_at, expires_at);
