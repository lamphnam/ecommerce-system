-- Phase 9 — Analytics Service initial schema.
-- Tables: analytics_events, processed_events.

CREATE TABLE analytics_events (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(100)   NOT NULL,
    event_type      VARCHAR(100)   NOT NULL,
    source_service  VARCHAR(100),
    aggregate_id    VARCHAR(100),
    user_id         BIGINT,
    correlation_id  VARCHAR(100),
    routing_key     VARCHAR(150),
    exchange_name   VARCHAR(150),
    payload_json    TEXT,
    occurred_at     TIMESTAMP WITH TIME ZONE,
    received_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_analytics_events_event_id ON analytics_events (event_id);
CREATE INDEX idx_analytics_events_event_type ON analytics_events (event_type);
CREATE INDEX idx_analytics_events_source_service ON analytics_events (source_service);
CREATE INDEX idx_analytics_events_aggregate_id ON analytics_events (aggregate_id);
CREATE INDEX idx_analytics_events_user_id ON analytics_events (user_id);
CREATE INDEX idx_analytics_events_occurred_at ON analytics_events (occurred_at);
CREATE INDEX idx_analytics_events_received_at ON analytics_events (received_at);

-- Idempotency tracking for inbound fan-out events. event_id is the primary key so a
-- duplicate insert raises a unique-constraint violation, which the listener treats
-- as "already processed, ack and skip".
CREATE TABLE processed_events (
    event_id     VARCHAR(100) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    consumer     VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);
