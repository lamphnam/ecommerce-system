package com.techlab.ecommerce.notification.entity;

import com.techlab.ecommerce.common.entity.BaseEntity;
import com.techlab.ecommerce.notification.enums.NotificationChannel;
import com.techlab.ecommerce.notification.enums.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    /** EventEnvelope.eventId of the notification.requested message. */
    @Column(name = "request_event_id", nullable = false, unique = true, length = 100)
    private String requestEventId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "channel", nullable = false, length = 40)
    private NotificationChannel channel;

    @Column(name = "template", nullable = false, length = 100)
    private String template;

    @Column(name = "notification_type", nullable = false, length = 100)
    private String type;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "payload_data", columnDefinition = "TEXT")
    private String payloadData;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 40)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "sent_at")
    private Instant sentAt;
}
