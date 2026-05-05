package com.techlab.ecommerce.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for the synchronous stock reservation endpoint.
 * Mirrors the fields of {@code OrderCreatedPayload} used in the async saga.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockRequest {

    @NotNull(message = "orderId is required")
    private Long orderId;

    private Long userId;

    @NotEmpty(message = "items must not be empty")
    @Valid
    private List<ReserveStockItemRequest> items;
}
