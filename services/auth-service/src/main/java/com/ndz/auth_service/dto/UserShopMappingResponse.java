package com.ndz.auth_service.dto;

import java.util.UUID;

public record UserShopMappingResponse(
        UUID id,
        UUID userId,
        UUID shopId
) {
}
