package com.techlab.ecommerce.analytics.repository;

import com.techlab.ecommerce.analytics.entity.AnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long>,
        JpaSpecificationExecutor<AnalyticsEvent> {

    long countByEventType(String eventType);
}
