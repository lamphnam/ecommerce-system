package com.techlab.ecommerce.order.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activates JPA auditing so {@code @CreatedDate} / {@code @LastModifiedDate}
 * on {@link com.techlab.ecommerce.common.entity.BaseEntity} are populated automatically.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
