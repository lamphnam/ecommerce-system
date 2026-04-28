package com.techlab.ecommerce.gateway.security;

import com.techlab.ecommerce.common.exception.UnauthorizedException;
import com.techlab.ecommerce.gateway.config.GatewayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

/**
 * Validates and parses JWT tokens issued by the (out-of-scope) auth service.
 * Returns the parsed claims so {@link JwtAuthFilter} can inject the
 * {@code X-User-*} headers downstream.
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final GatewayProperties.Jwt jwtConfig;

    public JwtService(GatewayProperties properties) {
        this.jwtConfig = properties.getJwt();
        if (this.jwtConfig.getSecret() == null || this.jwtConfig.getSecret().isBlank()) {
            throw new IllegalStateException("gateway.jwt.secret must be configured");
        }
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(toBase64(this.jwtConfig.getSecret())));
    }

    public Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .setAllowedClockSkewSeconds(jwtConfig.getClockSkewSeconds())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    public String extractUserId(Claims claims) {
        Object value = claims.get(jwtConfig.getUserIdClaim());
        return value != null ? value.toString() : null;
    }

    public String extractUsername(Claims claims) {
        Object value = claims.get(jwtConfig.getUsernameClaim());
        return value != null ? value.toString() : claims.getSubject();
    }

    public String extractRole(Claims claims) {
        Object value = claims.get(jwtConfig.getRoleClaim());
        return value != null ? value.toString() : null;
    }

    /**
     * Accept either a base64-encoded secret or a raw string. For raw strings, we
     * encode them so {@link Decoders#BASE64} works uniformly downstream.
     */
    private static String toBase64(String secret) {
        try {
            Decoders.BASE64.decode(secret);
            return secret;
        } catch (RuntimeException e) {
            return java.util.Base64.getEncoder()
                    .encodeToString(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
