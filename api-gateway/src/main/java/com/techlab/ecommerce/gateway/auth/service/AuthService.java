package com.techlab.ecommerce.gateway.auth.service;

import com.techlab.ecommerce.gateway.auth.dto.request.LoginRequest;
import com.techlab.ecommerce.gateway.auth.dto.request.RegisterRequest;
import com.techlab.ecommerce.gateway.auth.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
