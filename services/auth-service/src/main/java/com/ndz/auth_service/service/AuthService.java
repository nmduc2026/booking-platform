package com.ndz.auth_service.service;

import com.ndz.auth_service.dto.AuthResponse;
import com.ndz.auth_service.dto.LoginRequest;
import com.ndz.auth_service.dto.RefreshRequest;
import com.ndz.auth_service.dto.RegisterRequest;
import com.ndz.auth_service.dto.UserResponse;
import com.ndz.auth_service.entity.RefreshToken;
import com.ndz.auth_service.entity.Role;
import com.ndz.auth_service.entity.User;
import com.ndz.auth_service.entity.UserShopMapping;
import com.ndz.auth_service.entity.UserStatus;
import com.ndz.auth_service.exception.ApiException;
import com.ndz.auth_service.repository.RefreshTokenRepository;
import com.ndz.auth_service.repository.UserRepository;
import com.ndz.auth_service.repository.UserShopMappingRepository;
import com.ndz.auth_service.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserShopMappingRepository userShopMappingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserShopMappingRepository userShopMappingRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userShopMappingRepository = userShopMappingRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired or revoked");
        }

        User user = stored.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        stored.setRevoked(true);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        return UserResponse.from(user, shopIdsOf(user.getId()));
    }

    private AuthResponse issueTokens(User user) {
        List<UUID> shopIds = shopIdsOf(user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), shopIds);
        String refreshToken = createRefreshToken(user);
        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs(),
                UserResponse.from(user, shopIds)
        );
    }

    private String createRefreshToken(User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    private List<UUID> shopIdsOf(UUID userId) {
        return userShopMappingRepository.findByUserId(userId).stream()
                .map(UserShopMapping::getShopId)
                .toList();
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
