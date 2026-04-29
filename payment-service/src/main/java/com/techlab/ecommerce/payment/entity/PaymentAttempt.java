package com.techlab.ecommerce.payment.entity;

import com.techlab.ecommerce.common.entity.BaseEntity;
import com.techlab.ecommerce.payment.enums.PaymentAttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "payment")
@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "attempt_status", nullable = false, length = 40)
    private PaymentAttemptStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "provider_latency_ms")
    private Long providerLatencyMs;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;
}
