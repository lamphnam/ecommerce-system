package com.techlab.ecommerce.notification.service.impl;

import com.techlab.ecommerce.notification.dto.response.NotificationResponse;
import com.techlab.ecommerce.notification.enums.NotificationErrorMessage;
import com.techlab.ecommerce.notification.enums.NotificationStatus;
import com.techlab.ecommerce.notification.exception.NotificationException;
import com.techlab.ecommerce.notification.mapper.NotificationMapper;
import com.techlab.ecommerce.notification.repository.NotificationRepository;
import com.techlab.ecommerce.notification.service.NotificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationQueryServiceImpl implements NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotification(Long id) {
        return notificationRepository.findById(id)
                .map(notificationMapper::toDto)
                .orElseThrow(() -> new NotificationException(NotificationErrorMessage.NOTIFICATION_NOT_FOUND,
                        "Notification " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> listNotifications(NotificationStatus status, Pageable pageable) {
        Page<com.techlab.ecommerce.notification.entity.Notification> page = status == null
                ? notificationRepository.findAll(pageable)
                : notificationRepository.findByStatus(status, pageable);
        return page.map(notificationMapper::toDto);
    }
}
