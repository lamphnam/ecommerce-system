package com.techlab.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Debug/admin stock adjustment. For simplicity this sets the absolute stock
 * value instead of modelling warehouse receipts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustStockRequest {

    @NotNull
    @Min(0)
    private Integer stock;
}
