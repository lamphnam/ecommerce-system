-- Phase 6 — Inventory Service initial schema.
-- Tables: products, stock_reservations, processed_events.

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(80)    NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    price       NUMERIC(19, 2) NOT NULL CHECK (price >= 0),
    stock       INT            NOT NULL CHECK (stock >= 0),
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_products_sku ON products (sku);
CREATE INDEX idx_products_active ON products (active);

CREATE TABLE stock_reservations (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT       NOT NULL,
    product_id  BIGINT       NOT NULL,
    quantity    INT          NOT NULL CHECK (quantity > 0),
    status      VARCHAR(40)  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_reservations_product_id FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX idx_stock_reservations_order_id ON stock_reservations (order_id);
CREATE INDEX idx_stock_reservations_order_status ON stock_reservations (order_id, status);
CREATE INDEX idx_stock_reservations_product_id ON stock_reservations (product_id);

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
