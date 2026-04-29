package com.techlab.ecommerce.inventory.exception;

/**
 * Raised only when inserting into processed_events hits the event_id primary key.
 * The listener can safely ack these duplicates without confusing them with other
 * database integrity failures.
 */
public class DuplicateProcessedEventException extends RuntimeException {

    public DuplicateProcessedEventException(String eventId, Throwable cause) {
        super("Event already processed: " + eventId, cause);
    }
}
