package com.techlab.ecommerce.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    /** Three-letter ISO currency code, e.g. "USD". Defaults to USD if omitted. */
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter ISO code")
    private String currency;

    @NotEmpty(message = "items must contain at least one item")
    @Size(max = 50, message = "an order may contain at most 50 items")
    @Valid
    private List<CreateOrderItemRequest> items;
}
