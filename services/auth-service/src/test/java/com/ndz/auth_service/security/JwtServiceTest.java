package com.ndz.auth_service.security;

import com.ndz.auth_service.config.JwtProperties;
import com.ndz.auth_service.entity.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-that-is-long-enough-for-hs256-algorithm-123456",
                900_000L,
                604_800_000L
        );
        jwtService = new JwtService(properties);
    }

    @Test
    void generateAndParseAccessToken_containsExpectedClaims() {
        UUID userId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(
                userId,
                "manager@shop.com",
                Role.SHOP_MANAGER,
                List.of(shopId)
        );

        assertThat(jwtService.isValid(token)).isTrue();

        Claims claims = jwtService.parseClaims(token);
        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtService.extractEmail(claims)).isEqualTo("manager@shop.com");
        assertThat(jwtService.extractRole(claims)).isEqualTo(Role.SHOP_MANAGER);
        assertThat(jwtService.extractShopIds(claims)).containsExactly(shopId);
    }

    @Test
    void isValid_returnsFalseForTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@test.com", Role.USER, List.of());
        String tampered = token.substring(0, token.length() - 4) + "xxxx";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void parseClaims_rejectsTokenSignedWithDifferentSecret() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "user@test.com", Role.USER, List.of());

        JwtService other = new JwtService(new JwtProperties(
                "another-secret-key-that-is-long-enough-for-hs256-algorithm!!",
                900_000L,
                604_800_000L
        ));

        assertThat(other.isValid(token)).isFalse();
        assertThatThrownBy(() -> other.parseClaims(token)).isInstanceOf(RuntimeException.class);
    }
}
