package com.techlab.ecommerce.gateway.auth.controller;

import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.gateway.auth.dto.request.LoginRequest;
import com.techlab.ecommerce.gateway.auth.dto.request.RegisterRequest;
import com.techlab.ecommerce.gateway.auth.dto.response.AuthResponse;
import com.techlab.ecommerce.gateway.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Demo Auth", description = "Small api-gateway-owned auth API for local demos")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a demo user and return a JWT")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.register(request)));
    }

    @Operation(summary = "Login and return a JWT")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
