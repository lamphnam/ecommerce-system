package com.techlab.ecommerce.gateway.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 120)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @Size(max = 255)
    private String displayName;
}
