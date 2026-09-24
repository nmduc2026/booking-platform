package com.ndz.venue_service.dto;

import com.ndz.venue_service.entity.Shop;
import com.ndz.venue_service.entity.ShopStatus;

import java.time.Instant;
import java.util.UUID;

public record ShopResponse(
        UUID id,
        String name,
        String address,
        String description,
        ShopStatus status,
        Instant createdAt
) {
    public static ShopResponse from(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getAddress(),
                shop.getDescription(),
                shop.getStatus(),
                shop.getCreatedAt()
        );
    }
}
