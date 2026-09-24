CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255),
    phone           VARCHAR(20),
    role            VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE UNIQUE INDEX uk_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id),
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMP    NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE TABLE user_shop_mapping (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users (id),
    shop_id         UUID         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_user_shop_mapping_user_shop ON user_shop_mapping (user_id, shop_id);
