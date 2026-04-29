package com.techlab.ecommerce.notification.dto.response;

import com.techlab.ecommerce.notification.enums.NotificationChannel;
import com.techlab.ecommerce.notification.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String requestEventId;
    private Long userId;
    private Long orderId;
    private String recipient;
    private NotificationChannel channel;
    private String template;
    private String type;
    private String body;
    private String payloadData;
    private NotificationStatus status;
    private Integer attemptCount;
    private String lastError;
    private Instant sentAt;
    private Instant createdAt;
    private Instant updatedAt;
}
