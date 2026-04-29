package com.techlab.ecommerce.notification.repository;

import com.techlab.ecommerce.notification.entity.Notification;
import com.techlab.ecommerce.notification.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByRequestEventId(String requestEventId);

    Page<Notification> findByStatus(NotificationStatus status, Pageable pageable);
}
