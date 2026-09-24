package com.ndz.booking_service.dto;

import com.ndz.booking_service.entity.Booking;
import com.ndz.booking_service.entity.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID userId,
        UUID shopId,
        UUID slotId,
        BookingStatus status,
        BigDecimal amount,
        Instant expiresAt,
        Instant createdAt
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getShopId(),
                booking.getSlotId(),
                booking.getStatus(),
                booking.getAmount(),
                booking.getExpiresAt(),
                booking.getCreatedAt()
        );
    }
}
