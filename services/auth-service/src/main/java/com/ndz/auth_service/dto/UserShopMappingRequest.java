package com.ndz.auth_service.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserShopMappingRequest(
        @NotNull UUID userId,
        @NotNull UUID shopId
) {
}
