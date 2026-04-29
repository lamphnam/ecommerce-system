-- Phase 8 — Notification Service initial schema.
-- Tables: notifications, processed_events.

CREATE TABLE notifications (
    id               BIGSERIAL PRIMARY KEY,
    request_event_id VARCHAR(100)   NOT NULL,
    user_id          BIGINT,
    order_id         BIGINT,
    recipient        VARCHAR(255)   NOT NULL,
    channel          VARCHAR(40)    NOT NULL,
    template         VARCHAR(100)   NOT NULL,
    notification_type VARCHAR(100)  NOT NULL,
    body             TEXT,
    payload_data     TEXT,
    status           VARCHAR(40)    NOT NULL,
    attempt_count    INT            NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    last_error       VARCHAR(500),
    sent_at          TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    version          BIGINT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_notifications_request_event_id ON notifications (request_event_id);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_order_id ON notifications (order_id);

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
