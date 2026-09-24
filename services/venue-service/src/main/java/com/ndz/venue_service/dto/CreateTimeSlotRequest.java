package com.ndz.venue_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateTimeSlotRequest(
        @NotNull UUID resourceId,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @NotNull @DecimalMin("0.0") BigDecimal price
) {
}
