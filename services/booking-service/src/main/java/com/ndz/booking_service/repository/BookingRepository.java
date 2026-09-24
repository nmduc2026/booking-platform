package com.ndz.booking_service.repository;

import com.ndz.booking_service.entity.Booking;
import com.ndz.booking_service.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Booking> findByIdAndUserId(UUID id, UUID userId);

    boolean existsBySlotIdAndStatusIn(UUID slotId, List<BookingStatus> statuses);
}
