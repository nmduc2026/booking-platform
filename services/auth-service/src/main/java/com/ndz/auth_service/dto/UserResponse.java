package com.ndz.auth_service.dto;

import com.ndz.auth_service.entity.Role;
import com.ndz.auth_service.entity.User;
import com.ndz.auth_service.entity.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        Role role,
        UserStatus status,
        List<UUID> shopIds,
        Instant createdAt
) {
    public static UserResponse from(User user, List<UUID> shopIds) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                shopIds == null ? List.of() : shopIds,
                user.getCreatedAt()
        );
    }
}
