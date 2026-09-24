package com.ndz.venue_service.dto;

import com.ndz.venue_service.entity.SlotStatus;
import com.ndz.venue_service.entity.TimeSlot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TimeSlotResponse(
        UUID id,
        UUID shopId,
        UUID resourceId,
        Instant startTime,
        Instant endTime,
        BigDecimal price,
        SlotStatus status
) {
    public static TimeSlotResponse from(TimeSlot slot) {
        return new TimeSlotResponse(
                slot.getId(),
                slot.getShopId(),
                slot.getResourceId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getPrice(),
                slot.getStatus()
        );
    }
}
