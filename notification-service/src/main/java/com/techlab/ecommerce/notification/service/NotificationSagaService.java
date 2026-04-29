package com.techlab.ecommerce.notification.service;

import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.notification.messaging.payload.inbound.NotificationRequestedPayload;

public interface NotificationSagaService {

    void handleNotificationRequested(EventEnvelope<NotificationRequestedPayload> event);
}
