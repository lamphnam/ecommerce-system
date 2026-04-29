package com.techlab.ecommerce.gateway.auth.dto.response;

import com.techlab.ecommerce.gateway.auth.enums.GatewayUserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoUserResponse {

    private Long id;
    private String username;
    private String displayName;
    private GatewayUserRole role;
    private Boolean enabled;
}
