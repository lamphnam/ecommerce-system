package com.techlab.ecommerce.gateway.auth.service.impl;

import com.techlab.ecommerce.gateway.auth.dto.request.LoginRequest;
import com.techlab.ecommerce.gateway.auth.dto.request.RegisterRequest;
import com.techlab.ecommerce.gateway.auth.dto.response.AuthResponse;
import com.techlab.ecommerce.gateway.auth.dto.response.DemoUserResponse;
import com.techlab.ecommerce.gateway.auth.entity.GatewayUser;
import com.techlab.ecommerce.gateway.auth.enums.GatewayUserRole;
import com.techlab.ecommerce.gateway.auth.repository.GatewayUserRepository;
import com.techlab.ecommerce.gateway.auth.service.AuthService;
import com.techlab.ecommerce.gateway.enums.GatewayErrorMessage;
import com.techlab.ecommerce.gateway.exception.GatewayException;
import com.techlab.ecommerce.gateway.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final GatewayUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (userRepository.existsByUsername(username)) {
            throw new GatewayException(GatewayErrorMessage.USERNAME_ALREADY_EXISTS,
                    "Username already exists: " + username);
        }

        GatewayUser user = GatewayUser.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(normalizeDisplayName(request.getDisplayName(), username))
                .role(GatewayUserRole.USER)
                .enabled(true)
                .build();

        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException duplicateRace) {
            throw new GatewayException(GatewayErrorMessage.USERNAME_ALREADY_EXISTS,
                    "Username already exists: " + username);
        }

        log.info("Registered demo gateway user id={} username={}", user.getId(), user.getUsername());
        return toAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        GatewayUser user = userRepository.findByUsername(username)
                .orElseThrow(this::invalidCredentials);
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new GatewayException(GatewayErrorMessage.USER_DISABLED);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(GatewayUser user) {
        return AuthResponse.builder()
                .tokenType(TOKEN_TYPE)
                .accessToken(jwtService.issueToken(user))
                .expiresInSeconds(jwtService.expiresInSeconds())
                .user(toUserResponse(user))
                .build();
    }

    private static DemoUserResponse toUserResponse(GatewayUser user) {
        return DemoUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .build();
    }

    private GatewayException invalidCredentials() {
        return new GatewayException(GatewayErrorMessage.INVALID_CREDENTIALS);
    }

    private static String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }

    private static String normalizeDisplayName(String displayName, String fallback) {
        return (displayName == null || displayName.isBlank()) ? fallback : displayName.trim();
    }
}
