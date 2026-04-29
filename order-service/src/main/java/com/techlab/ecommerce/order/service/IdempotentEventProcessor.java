package com.techlab.ecommerce.order.service;

import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.order.entity.ProcessedEvent;
import com.techlab.ecommerce.order.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tracks consumed events in {@code processed_events} so duplicate broker deliveries
 * are skipped. Insert is the contract:
 *
 * <ol>
 *   <li>caller invokes {@link #markProcessed(EventEnvelope, String)} <em>inside</em>
 *       its handler transaction;</li>
 *   <li>the unique PK on {@code event_id} either inserts a fresh row (handler runs
 *       through and commits), or raises {@link DataIntegrityViolationException}
 *       (the wrapper {@link #processOnce} catches it, returns {@code false}, and
 *       the listener acks without re-running the side effect).</li>
 * </ol>
 *
 * <p>Using {@link Propagation#REQUIRES_NEW} on {@link #isAlreadyProcessed} would
 * also work but adds a round-trip; here we rely on the PK constraint for the
 * happy path which is one statement.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentEventProcessor {

    private final ProcessedEventRepository repository;

    /**
     * Inserts a marker row for the given event. Throws
     * {@link DataIntegrityViolationException} if the event was already processed.
     * Must be called from within the handler's transaction so a rollback also
     * undoes the marker.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void markProcessed(EventEnvelope<?> event, String consumer) {
        ProcessedEvent marker = ProcessedEvent.builder()
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .consumer(consumer)
                .build();
        repository.saveAndFlush(marker);
    }

    /**
     * Optional pre-check used by listeners that want to ack-and-skip without
     * starting a transaction at all. The PK insert in
     * {@link #markProcessed(EventEnvelope, String)} is still the source of truth.
     */
    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String eventId) {
        return repository.existsById(eventId);
    }
}
