package com.techlab.ecommerce.payment.controller;

import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.payment.dto.request.ProcessPaymentRequest;
import com.techlab.ecommerce.payment.dto.response.PaymentResponse;
import com.techlab.ecommerce.payment.service.PaymentQueryService;
import com.techlab.ecommerce.payment.service.PaymentSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Payments", description = "Read/debug payment records")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentQueryService paymentQueryService;
    private final PaymentSyncService paymentSyncService;

    @Operation(summary = "Process a payment synchronously (experiment baseline only)",
            description = "Creates a payment record, calls the simulated provider, and returns the result. "
                    + "Used by the sync order flow for experiment comparison — NOT for production use.")
    @PostMapping("/process")
    public ApiResponse<PaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request) {
        return ApiResponse.ok(paymentSyncService.processPayment(request));
    }

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
