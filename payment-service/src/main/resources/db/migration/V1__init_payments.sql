-- Phase 7 — Payment Service initial schema.
-- Tables: payments, payment_attempts, processed_events.

CREATE TABLE payments (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT          NOT NULL,
    status             VARCHAR(40)     NOT NULL,
    amount             NUMERIC(19, 2)  NOT NULL CHECK (amount >= 0),
    currency           CHAR(3)         NOT NULL DEFAULT 'USD',
    payment_method     VARCHAR(60),
    provider_reference VARCHAR(120),
    failure_reason     VARCHAR(500),
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT          NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_provider_reference ON payments (provider_reference);

CREATE TABLE payment_attempts (
    id                  BIGSERIAL PRIMARY KEY,
    payment_id          BIGINT         NOT NULL,
    attempt_number      INT            NOT NULL CHECK (attempt_number > 0),
    attempt_status      VARCHAR(40)    NOT NULL,
    error_message       VARCHAR(500),
    provider_latency_ms BIGINT,
    attempted_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    version             BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_payment_attempts_payment_id FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE UNIQUE INDEX uk_payment_attempts_payment_attempt_number
    ON payment_attempts (payment_id, attempt_number);
CREATE INDEX idx_payment_attempts_payment_id ON payment_attempts (payment_id);

-- Idempotency tracking for inbound saga events. event_id is the primary key so a
-- duplicate insert raises a unique-constraint violation, which the listener treats
-- as "already processed, ack and skip".
CREATE TABLE processed_events (
    event_id     VARCHAR(100) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    consumer     VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);
