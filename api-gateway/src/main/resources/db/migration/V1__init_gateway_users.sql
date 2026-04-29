-- Demo authentication schema owned by api-gateway.
-- This is intentionally small and local to the gateway; it is not a full user-profile service.

CREATE TABLE gateway_users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(120) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    display_name  VARCHAR(255),
    role          VARCHAR(40)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_gateway_users_username ON gateway_users (username);
CREATE INDEX idx_gateway_users_enabled ON gateway_users (enabled);
