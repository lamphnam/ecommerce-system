package com.techlab.ecommerce.notification.service;

import com.techlab.ecommerce.notification.dto.response.NotificationResponse;
import com.techlab.ecommerce.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationQueryService {

    NotificationResponse getNotification(Long id);

    Page<NotificationResponse> listNotifications(NotificationStatus status, Pageable pageable);
}
