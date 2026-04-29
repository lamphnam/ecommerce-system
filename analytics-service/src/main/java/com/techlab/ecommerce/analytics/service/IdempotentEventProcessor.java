package com.techlab.ecommerce.analytics.service;

import com.techlab.ecommerce.analytics.entity.ProcessedEvent;
import com.techlab.ecommerce.analytics.exception.DuplicateProcessedEventException;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

/**
 * Inserts into processed_events as the idempotency check for inbound messages.
 * Converts only duplicate marker-key violations into DuplicateProcessedEventException;
 * unrelated database integrity errors remain real failures and go to the DLQ.
 */
@Component
public class IdempotentEventProcessor {

    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void markProcessed(EventEnvelope<?> event, String consumer) {
        if (event == null || event.getEventId() == null || event.getEventId().isBlank()) {
            throw new IllegalArgumentException("Inbound event must contain a non-blank eventId");
        }
        ProcessedEvent marker = ProcessedEvent.builder()
                .eventId(event.getEventId())
                .eventType(nonBlankOrDefault(event.getEventType(), "<unknown>"))
                .consumer(consumer)
                .build();
        try {
            entityManager.persist(marker);
            entityManager.flush();
        } catch (RuntimeException ex) {
            if (isDuplicateKey(ex)) {
                throw new DuplicateProcessedEventException(event.getEventId(), ex);
            }
            throw ex;
        }
    }

    private static boolean isDuplicateKey(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraint) {
                if (UNIQUE_VIOLATION_SQL_STATE.equals(constraint.getSQLState())) {
                    return true;
                }
                String constraintName = constraint.getConstraintName();
                if (constraintName != null && constraintName.toLowerCase().contains("processed_events")) {
                    return true;
                }
            }
            if (current instanceof SQLException sql
                    && UNIQUE_VIOLATION_SQL_STATE.equals(sql.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
