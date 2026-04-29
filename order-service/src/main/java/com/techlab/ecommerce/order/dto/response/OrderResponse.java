package com.techlab.ecommerce.order.dto.response;

import com.techlab.ecommerce.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String currency;
    private String failureReason;
    private String idempotencyKey;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OrderItemResponse> items;
}
