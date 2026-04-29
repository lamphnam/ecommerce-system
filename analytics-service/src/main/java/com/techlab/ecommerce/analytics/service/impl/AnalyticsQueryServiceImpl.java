package com.techlab.ecommerce.analytics.service.impl;

import com.techlab.ecommerce.analytics.dto.response.AnalyticsEventResponse;
import com.techlab.ecommerce.analytics.dto.response.OrderEventsSummaryResponse;
import com.techlab.ecommerce.analytics.entity.AnalyticsEvent;
import com.techlab.ecommerce.analytics.enums.AnalyticsErrorMessage;
import com.techlab.ecommerce.analytics.exception.AnalyticsException;
import com.techlab.ecommerce.analytics.mapper.AnalyticsEventMapper;
import com.techlab.ecommerce.analytics.repository.AnalyticsEventRepository;
import com.techlab.ecommerce.analytics.service.AnalyticsQueryService;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final AnalyticsEventMapper analyticsEventMapper;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsEventResponse getEvent(Long id) {
        return analyticsEventRepository.findById(id)
                .map(analyticsEventMapper::toDto)
                .orElseThrow(() -> new AnalyticsException(AnalyticsErrorMessage.EVENT_NOT_FOUND,
                        "Analytics event " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnalyticsEventResponse> listEvents(String eventType,
                                                   String sourceService,
                                                   String aggregateId,
                                                   Long userId,
                                                   Instant from,
                                                   Instant to,
                                                   Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new AnalyticsException(AnalyticsErrorMessage.INVALID_TIME_RANGE,
                    "from must be before or equal to to");
        }
        return analyticsEventRepository.findAll(
                filterSpec(eventType, sourceService, aggregateId, userId, from, to), pageable)
                .map(analyticsEventMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderEventsSummaryResponse orderSummary() {
        return OrderEventsSummaryResponse.builder()
                .orderCreatedCount(analyticsEventRepository.countByEventType(RoutingKeys.ORDER_CREATED))
                .paymentSucceededCount(analyticsEventRepository.countByEventType(RoutingKeys.PAYMENT_SUCCEEDED))
                .paymentFailedCount(analyticsEventRepository.countByEventType(RoutingKeys.PAYMENT_FAILED))
                .inventoryFailedCount(analyticsEventRepository.countByEventType(RoutingKeys.INVENTORY_FAILED))
                .notificationFailedCount(analyticsEventRepository.countByEventType(RoutingKeys.NOTIFICATION_FAILED))
                .build();
    }

    private static Specification<AnalyticsEvent> filterSpec(String eventType,
                                                            String sourceService,
                                                            String aggregateId,
                                                            Long userId,
                                                            Instant from,
                                                            Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(eventType)) {
                predicates.add(cb.equal(root.get("eventType"), eventType));
            }
            if (hasText(sourceService)) {
                predicates.add(cb.equal(root.get("sourceService"), sourceService));
            }
            if (hasText(aggregateId)) {
                predicates.add(cb.equal(root.get("aggregateId"), aggregateId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
