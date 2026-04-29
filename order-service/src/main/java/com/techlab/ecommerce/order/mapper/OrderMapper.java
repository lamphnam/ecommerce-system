package com.techlab.ecommerce.order.mapper;

import com.techlab.ecommerce.common.mapper.SuperConverter;
import com.techlab.ecommerce.order.dto.response.OrderItemResponse;
import com.techlab.ecommerce.order.dto.response.OrderResponse;
import com.techlab.ecommerce.order.entity.Order;
import com.techlab.ecommerce.order.entity.OrderItem;
import com.techlab.ecommerce.order.messaging.payload.OrderItemPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper extends SuperConverter<OrderResponse, Order> {

    @Override
    public OrderResponse toDto(Order entity) {
        if (entity == null) {
            return null;
        }
        List<OrderItemResponse> items = entity.getItems() == null ? List.of()
                : entity.getItems().stream().map(this::toItemDto).toList();
        return OrderResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .failureReason(entity.getFailureReason())
                .idempotencyKey(entity.getIdempotencyKey())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .items(items)
                .build();
    }

    @Override
    public Order toEntity(OrderResponse dto) {
        // Order entities are constructed by the service from the create-request DTO,
        // not rebuilt from the response DTO. This direction is intentionally unused.
        throw new UnsupportedOperationException(
                "OrderResponse → Order is not supported; orders are constructed from CreateOrderRequest");
    }

    public OrderItemResponse toItemDto(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }

    public OrderItemPayload toItemPayload(OrderItem item) {
        return OrderItemPayload.builder()
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}
