package com.techlab.ecommerce.gateway.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String tokenType;
    private String accessToken;
    private Long expiresInSeconds;
    private DemoUserResponse user;
}
