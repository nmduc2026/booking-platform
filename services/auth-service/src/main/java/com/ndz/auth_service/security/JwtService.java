package com.ndz.auth_service.security;

import com.ndz.auth_service.config.JwtProperties;
import com.ndz.auth_service.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UUID userId, String email, Role role, List<UUID> shopIds) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.accessTokenExpirationMs());

        List<String> shopIdStrings = shopIds == null
                ? List.of()
                : shopIds.stream().map(UUID::toString).toList();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim("shopIds", shopIdStrings)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public String extractEmail(Claims claims) {
        return claims.get("email", String.class);
    }

    public Role extractRole(Claims claims) {
        return Role.valueOf(claims.get("role", String.class));
    }

    public List<UUID> extractShopIds(Claims claims) {
        Object raw = claims.get("shopIds");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(Object::toString)
                .map(UUID::fromString)
                .toList();
    }

    public long getAccessTokenExpirationMs() {
        return properties.accessTokenExpirationMs();
    }

    public long getRefreshTokenExpirationMs() {
        return properties.refreshTokenExpirationMs();
    }
}
