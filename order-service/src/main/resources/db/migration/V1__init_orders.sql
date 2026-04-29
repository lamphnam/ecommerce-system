-- Phase 5 — Order Service initial schema.
-- Tables: orders, order_items, processed_events.

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    status          VARCHAR(40)  NOT NULL,
    total_amount    NUMERIC(19, 2) NOT NULL,
    currency        CHAR(3)      NOT NULL DEFAULT 'USD',
    failure_reason  VARCHAR(500),
    idempotency_key VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0
);

-- Same idempotency key is allowed for different users; rejected within one user.
CREATE UNIQUE INDEX uk_orders_user_idempotency_key
    ON orders (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status  ON orders (status);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT          NOT NULL,
    product_id  BIGINT          NOT NULL,
    quantity    INT             NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(19, 2)  NOT NULL CHECK (unit_price >= 0),
    subtotal    NUMERIC(19, 2)  NOT NULL CHECK (subtotal  >= 0),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_order_items_order_id FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

-- Idempotency tracking for inbound saga events. event_id is the primary key so a
-- duplicate insert raises a constraint violation, which the consumer treats as
-- "already processed, ack and skip".
CREATE TABLE processed_events (
    event_id     VARCHAR(100) PRIMARY KEY,
    event_type   VARCHAR(100) NOT NULL,
    consumer     VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_events_processed_at ON processed_events (processed_at);
