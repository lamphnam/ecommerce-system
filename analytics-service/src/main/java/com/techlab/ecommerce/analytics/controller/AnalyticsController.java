package com.techlab.ecommerce.analytics.controller;

import com.techlab.ecommerce.analytics.dto.response.AnalyticsEventResponse;
import com.techlab.ecommerce.analytics.dto.response.OrderEventsSummaryResponse;
import com.techlab.ecommerce.analytics.service.AnalyticsQueryService;
import com.techlab.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Tag(name = "Analytics", description = "Read/debug observed business events")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    @Operation(summary = "List observed business events")
    @GetMapping("/events")
    public ApiResponse<Page<AnalyticsEventResponse>> listEvents(
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "sourceService", required = false) String sourceService,
            @RequestParam(value = "aggregateId", required = false) String aggregateId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(analyticsQueryService.listEvents(
                eventType, sourceService, aggregateId, userId, from, to, pageable));
    }

    @Operation(summary = "Get one observed business event")
    @GetMapping("/events/{id}")
    public ApiResponse<AnalyticsEventResponse> getEvent(@PathVariable Long id) {
        return ApiResponse.ok(analyticsQueryService.getEvent(id));
    }

    @Operation(summary = "Simple order-saga event summary")
    @GetMapping("/summary/orders")
    public ApiResponse<OrderEventsSummaryResponse> orderSummary() {
        return ApiResponse.ok(analyticsQueryService.orderSummary());
    }
}
