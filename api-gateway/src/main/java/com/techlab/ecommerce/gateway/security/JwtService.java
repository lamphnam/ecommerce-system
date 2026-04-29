package com.techlab.ecommerce.gateway.security;

import com.techlab.ecommerce.common.exception.UnauthorizedException;
import com.techlab.ecommerce.gateway.config.GatewayProperties;
import com.techlab.ecommerce.gateway.auth.entity.GatewayUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Issues, validates, and parses demo JWT tokens for api-gateway.
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

    public String issueToken(GatewayUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpirationSeconds() * 1000L);
        var builder = Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .claim(jwtConfig.getUserIdClaim(), user.getId())
                .claim(jwtConfig.getRoleClaim(), user.getRole().name());
        if (!"sub".equals(jwtConfig.getUsernameClaim())) {
            builder.claim(jwtConfig.getUsernameClaim(), user.getUsername());
        }
        return builder.signWith(signingKey, SignatureAlgorithm.HS256).compact();
    }

    public long expiresInSeconds() {
        return jwtConfig.getExpirationSeconds();
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
