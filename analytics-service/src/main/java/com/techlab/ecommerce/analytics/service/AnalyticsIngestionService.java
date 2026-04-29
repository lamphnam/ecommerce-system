package com.techlab.ecommerce.analytics.service;

import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;

public interface AnalyticsIngestionService {

    void handleEvent(EventEnvelope<Object> event, String routingKey, String exchangeName);
}
