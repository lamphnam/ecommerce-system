package com.techlab.ecommerce.gateway.auth;

import com.techlab.ecommerce.gateway.auth.dto.request.LoginRequest;
import com.techlab.ecommerce.gateway.auth.dto.request.RegisterRequest;
import com.techlab.ecommerce.gateway.auth.dto.response.AuthResponse;
import com.techlab.ecommerce.gateway.auth.entity.GatewayUser;
import com.techlab.ecommerce.gateway.auth.enums.GatewayUserRole;
import com.techlab.ecommerce.gateway.auth.repository.GatewayUserRepository;
import com.techlab.ecommerce.gateway.auth.service.AuthService;
import com.techlab.ecommerce.gateway.exception.GatewayException;
import com.techlab.ecommerce.gateway.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceImplTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private GatewayUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void register_createsUserWithHashedPasswordAndReturnsJwt() {
        String username = uniqueUsername();

        AuthResponse response = authService.register(RegisterRequest.builder()
                .username(username)
                .password("secret123")
                .displayName("Demo User")
                .build());

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getExpiresInSeconds()).isEqualTo(3600L);
        assertThat(response.getUser().getUsername()).isEqualTo(username);
        assertThat(response.getUser().getRole()).isEqualTo(GatewayUserRole.USER);

        GatewayUser user = userRepository.findByUsername(username).orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", user.getPasswordHash())).isTrue();

        Claims claims = jwtService.parse(response.getAccessToken());
        assertThat(jwtService.extractUserId(claims)).isEqualTo(user.getId().toString());
        assertThat(jwtService.extractUsername(claims)).isEqualTo(username);
        assertThat(jwtService.extractRole(claims)).isEqualTo("USER");
    }

    @Test
    void register_duplicateUsernameIsRejected() {
        String username = uniqueUsername();
        RegisterRequest request = RegisterRequest.builder()
                .username(username)
                .password("secret123")
                .displayName("Demo")
                .build();
        authService.register(request);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(GatewayException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void login_correctPasswordReturnsJwt() {
        String username = uniqueUsername();
        authService.register(RegisterRequest.builder()
                .username(username)
                .password("secret123")
                .displayName("Demo")
                .build());

        AuthResponse response = authService.login(LoginRequest.builder()
                .username(username)
                .password("secret123")
                .build());

        assertThat(response.getAccessToken()).isNotBlank();
        Claims claims = jwtService.parse(response.getAccessToken());
        assertThat(jwtService.extractUsername(claims)).isEqualTo(username);
        assertThat(jwtService.extractRole(claims)).isEqualTo("USER");
    }

    @Test
    void login_wrongPasswordFails() {
        String username = uniqueUsername();
        authService.register(RegisterRequest.builder()
                .username(username)
                .password("secret123")
                .displayName("Demo")
                .build());

        assertThatThrownBy(() -> authService.login(LoginRequest.builder()
                .username(username)
                .password("wrong-password")
                .build()))
                .isInstanceOf(GatewayException.class)
                .hasMessageContaining("Invalid username or password");
    }

    private static String uniqueUsername() {
        return "demo-" + UUID.randomUUID() + "@example.local";
    }
}
