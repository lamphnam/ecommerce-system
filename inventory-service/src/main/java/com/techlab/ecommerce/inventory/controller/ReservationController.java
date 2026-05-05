package com.techlab.ecommerce.inventory.controller;

import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.inventory.dto.request.ReserveStockRequest;
import com.techlab.ecommerce.inventory.dto.response.ReservationResponse;
import com.techlab.ecommerce.inventory.service.InventorySyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Synchronous stock reservation endpoint for the experiment baseline.
 * Separated from {@link ProductController} to keep admin/debug APIs distinct.
 */
@Tag(name = "Inventory Reservations", description = "Synchronous stock reservation (experiment baseline)")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class ReservationController {

    private final InventorySyncService inventorySyncService;

    @Operation(summary = "Reserve stock synchronously (experiment baseline only)",
            description = "Performs atomic stock decrement for each item. "
                    + "Used by the sync order flow for experiment comparison — NOT for production use.")
    @PostMapping("/reserve")
    public ApiResponse<ReservationResponse> reserveStock(
            @Valid @RequestBody ReserveStockRequest request) {
        return ApiResponse.ok(inventorySyncService.reserveStock(request));
    }
}
