package com.techlab.ecommerce.payment.controller;

import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.payment.dto.response.PaymentResponse;
import com.techlab.ecommerce.payment.service.PaymentQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments", description = "Read/debug payment records")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentQueryService paymentQueryService;

    @Operation(summary = "Get payment by id")
    @GetMapping("/{id}")
    public ApiResponse<PaymentResponse> getPayment(@PathVariable Long id) {
        return ApiResponse.ok(paymentQueryService.getPayment(id));
    }

    @Operation(summary = "List payments, optionally filtered by orderId")
    @GetMapping
    public ApiResponse<Page<PaymentResponse>> listPayments(
            @RequestParam(value = "orderId", required = false) Long orderId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(paymentQueryService.listPayments(orderId, pageable));
    }
}
