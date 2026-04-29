package com.techlab.ecommerce.payment.mapper;

import com.techlab.ecommerce.common.mapper.SuperConverter;
import com.techlab.ecommerce.payment.dto.response.PaymentAttemptResponse;
import com.techlab.ecommerce.payment.dto.response.PaymentResponse;
import com.techlab.ecommerce.payment.entity.Payment;
import com.techlab.ecommerce.payment.entity.PaymentAttempt;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class PaymentMapper extends SuperConverter<PaymentResponse, Payment> {

    @Override
    public PaymentResponse toDto(Payment entity) {
        if (entity == null) {
            return null;
        }
        List<PaymentAttemptResponse> attempts = entity.getAttempts() == null ? List.of()
                : entity.getAttempts().stream()
                .sorted(Comparator.comparing(PaymentAttempt::getAttemptNumber))
                .map(this::toAttemptDto)
                .toList();
        return PaymentResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .status(entity.getStatus())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentMethod(entity.getPaymentMethod())
                .providerReference(entity.getProviderReference())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .attempts(attempts)
                .build();
    }

    @Override
    public Payment toEntity(PaymentResponse dto) {
        throw new UnsupportedOperationException(
                "PaymentResponse → Payment is not supported; payments are built from payment.requested events");
    }

    public PaymentAttemptResponse toAttemptDto(PaymentAttempt attempt) {
        return PaymentAttemptResponse.builder()
                .id(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .status(attempt.getStatus())
                .errorMessage(attempt.getErrorMessage())
                .providerLatencyMs(attempt.getProviderLatencyMs())
                .attemptedAt(attempt.getAttemptedAt())
                .build();
    }
}
