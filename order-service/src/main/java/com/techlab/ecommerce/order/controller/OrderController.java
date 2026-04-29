package com.techlab.ecommerce.order.controller;

import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.common.enums.CommonErrorMessage;
import com.techlab.ecommerce.common.exception.UnauthorizedException;
import com.techlab.ecommerce.common.security.UserContextHolder;
import com.techlab.ecommerce.order.dto.request.CreateOrderRequest;
import com.techlab.ecommerce.order.dto.response.OrderResponse;
import com.techlab.ecommerce.order.enums.OrderStatus;
import com.techlab.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST API for the Order Service. The current user is read from
 * {@link UserContextHolder} (populated by {@code UserContextFilter} from the
 * {@code X-User-*} headers the API Gateway injects after JWT validation).
 *
 * <p>POST is async-shaped: it persists the order, publishes {@code order.created},
 * and returns {@code 202 Accepted} with the order in {@code PENDING} state. The
 * saga progresses asynchronously over RabbitMQ.
 */
@Tag(name = "Orders", description = "Create and read orders")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final OrderService orderService;

    @Operation(summary = "Create a new order",
            description = "Persists the order in PENDING state, publishes order.created, and returns immediately. "
                    + "The saga progresses asynchronously. Pass an Idempotency-Key header to make replays safe.")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
        Long userId = currentUserId();
        OrderResponse created = orderService.createOrder(request, userId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.ok(created));
    }

    @Operation(summary = "Get an order by id (current user only)")
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long id) {
        Long userId = currentUserId();
        return ApiResponse.ok(orderService.getOrder(id, userId));
    }

    @Operation(summary = "List the current user's orders, optionally filtered by status")
    @GetMapping
    public ApiResponse<Page<OrderResponse>> listOrders(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = currentUserId();
        return ApiResponse.ok(orderService.listOrders(userId, status, pageable));
    }

    private static Long currentUserId() {
        Long userId = UserContextHolder.currentUserId();
        if (userId == null) {
            throw new UnauthorizedException(CommonErrorMessage.MISSING_USER_CONTEXT);
        }
        return userId;
    }
}
