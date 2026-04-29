package com.techlab.ecommerce.inventory.controller;

import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.inventory.dto.request.AdjustStockRequest;
import com.techlab.ecommerce.inventory.dto.request.CreateProductRequest;
import com.techlab.ecommerce.inventory.dto.response.ProductResponse;
import com.techlab.ecommerce.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inventory", description = "Admin/debug product stock APIs")
@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a product / seed initial stock")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResponse created = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @Operation(summary = "Get product stock/details")
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long id) {
        return ApiResponse.ok(productService.getProduct(id));
    }

    @Operation(summary = "Set product stock for local smoke tests")
    @PatchMapping("/{id}/stock")
    public ApiResponse<ProductResponse> adjustStock(@PathVariable Long id,
                                                    @Valid @RequestBody AdjustStockRequest request) {
        return ApiResponse.ok(productService.adjustStock(id, request));
    }
}
