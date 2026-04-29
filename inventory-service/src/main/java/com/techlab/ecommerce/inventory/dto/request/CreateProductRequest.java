package com.techlab.ecommerce.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {

    @NotBlank
    @Size(max = 80)
    private String sku;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal price;

    @NotNull
    @Min(0)
    private Integer stock;

    /** Optional; defaults to true when omitted. */
    private Boolean active;
}
