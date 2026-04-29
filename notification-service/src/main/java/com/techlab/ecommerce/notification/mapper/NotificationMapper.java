package com.techlab.ecommerce.notification.mapper;

import com.techlab.ecommerce.common.mapper.SuperConverter;
import com.techlab.ecommerce.notification.dto.response.NotificationResponse;
import com.techlab.ecommerce.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper extends SuperConverter<NotificationResponse, Notification> {

    @Override
    public NotificationResponse toDto(Notification entity) {
        if (entity == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(entity.getId())
                .requestEventId(entity.getRequestEventId())
                .userId(entity.getUserId())
                .orderId(entity.getOrderId())
                .recipient(entity.getRecipient())
                .channel(entity.getChannel())
                .template(entity.getTemplate())
                .type(entity.getType())
                .body(entity.getBody())
                .payloadData(entity.getPayloadData())
                .status(entity.getStatus())
                .attemptCount(entity.getAttemptCount())
                .lastError(entity.getLastError())
                .sentAt(entity.getSentAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public Notification toEntity(NotificationResponse dto) {
        throw new UnsupportedOperationException(
                "NotificationResponse → Notification is not supported; notifications are built from notification.requested events");
    }
}
