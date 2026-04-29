package com.techlab.ecommerce.order.enums;

/**
 * Lifecycle of an Order in the saga.
 *
 * <pre>
 *   PENDING ─► INVENTORY_RESERVED ─► CONFIRMED
 *      │              │                 ▲
 *      │              ▼                 │
 *      └────────────► FAILED            └── (terminal)
 *                       ▲
 *      CANCELLED ◄──────┘  (manual / admin)
 * </pre>
 *
 * <ul>
 *   <li>{@link #PENDING} — order persisted, {@code order.created} published, awaiting inventory.</li>
 *   <li>{@link #INVENTORY_RESERVED} — stock reserved, {@code payment.requested} published, awaiting payment.</li>
 *   <li>{@link #CONFIRMED} — payment captured, terminal happy path.</li>
 *   <li>{@link #FAILED} — inventory or payment rejected the order; {@code failureReason} is set.</li>
 *   <li>{@link #CANCELLED} — administrative cancellation (not used in Phase 5 flow yet).</li>
 * </ul>
 */
public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    CONFIRMED,
    FAILED,
    CANCELLED
}
