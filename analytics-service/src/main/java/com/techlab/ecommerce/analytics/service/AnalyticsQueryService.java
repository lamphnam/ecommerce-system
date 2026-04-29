package com.techlab.ecommerce.analytics.service;

import com.techlab.ecommerce.analytics.dto.response.AnalyticsEventResponse;
import com.techlab.ecommerce.analytics.dto.response.OrderEventsSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface AnalyticsQueryService {

    AnalyticsEventResponse getEvent(Long id);

    Page<AnalyticsEventResponse> listEvents(String eventType,
                                            String sourceService,
                                            String aggregateId,
                                            Long userId,
                                            Instant from,
                                            Instant to,
                                            Pageable pageable);

    OrderEventsSummaryResponse orderSummary();
}
