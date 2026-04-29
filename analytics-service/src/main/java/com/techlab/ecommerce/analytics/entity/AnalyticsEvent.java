package com.techlab.ecommerce.analytics.entity;

import com.techlab.ecommerce.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analytics_events")
public class AnalyticsEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "source_service", length = 100)
    private String sourceService;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "routing_key", length = 150)
    private String routingKey;

    @Column(name = "exchange_name", length = 150)
    private String exchangeName;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
