package com.ndz.venue_service.repository;

import com.ndz.venue_service.entity.SlotStatus;
import com.ndz.venue_service.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {

    boolean existsByResourceIdAndStartTime(UUID resourceId, Instant startTime);

    List<TimeSlot> findByShopIdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            UUID shopId,
            SlotStatus status,
            Instant startInclusive,
            Instant endExclusive
    );
}
