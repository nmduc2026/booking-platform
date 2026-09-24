package com.ndz.venue_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShopRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 500) String address,
        String description
) {
}
