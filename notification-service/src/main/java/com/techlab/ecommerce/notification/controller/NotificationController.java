package com.techlab.ecommerce.notification.controller;

import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.notification.dto.response.NotificationResponse;
import com.techlab.ecommerce.notification.enums.NotificationStatus;
import com.techlab.ecommerce.notification.service.NotificationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications", description = "Read/debug notification records")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;

    @Operation(summary = "Get notification by id")
    @GetMapping("/{id}")
    public ApiResponse<NotificationResponse> getNotification(@PathVariable Long id) {
        return ApiResponse.ok(notificationQueryService.getNotification(id));
    }

    @Operation(summary = "List notifications, optionally filtered by status")
    @GetMapping
    public ApiResponse<Page<NotificationResponse>> listNotifications(
            @RequestParam(value = "status", required = false) NotificationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(notificationQueryService.listNotifications(status, pageable));
    }
}
