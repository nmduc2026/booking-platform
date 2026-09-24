package com.ndz.booking_service.client;

import java.math.BigDecimal;
import java.util.UUID;

public record VenueSlotResponse(
        UUID id,
        UUID shopId,
        UUID resourceId,
        BigDecimal price,
        String status
) {
}
