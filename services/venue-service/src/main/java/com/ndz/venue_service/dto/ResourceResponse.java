package com.ndz.venue_service.dto;

import com.ndz.venue_service.entity.Resource;
import com.ndz.venue_service.entity.ResourceType;

import java.time.Instant;
import java.util.UUID;

public record ResourceResponse(
        UUID id,
        UUID shopId,
        String name,
        ResourceType type,
        Instant createdAt
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getShopId(),
                resource.getName(),
                resource.getType(),
                resource.getCreatedAt()
        );
    }
}
