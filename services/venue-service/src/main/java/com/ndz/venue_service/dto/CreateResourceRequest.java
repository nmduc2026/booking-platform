package com.ndz.venue_service.dto;

import com.ndz.venue_service.entity.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateResourceRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull ResourceType type
) {
}
