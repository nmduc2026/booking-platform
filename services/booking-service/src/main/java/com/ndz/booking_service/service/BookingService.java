package com.ndz.booking_service.service;

import com.ndz.booking_service.client.VenueClient;
import com.ndz.booking_service.client.VenueSlotResponse;
import com.ndz.booking_service.config.BookingProperties;
import com.ndz.booking_service.dto.BookingResponse;
import com.ndz.booking_service.dto.CreateBookingRequest;
import com.ndz.booking_service.entity.Booking;
import com.ndz.booking_service.entity.BookingStatus;
import com.ndz.booking_service.entity.Role;
import com.ndz.booking_service.exception.ApiException;
import com.ndz.booking_service.outbox.OutboxService;
import com.ndz.booking_service.repository.BookingRepository;
import com.ndz.booking_service.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.CANCELLING
    );

    private final BookingRepository bookingRepository;
    private final VenueClient venueClient;
    private final BookingProperties bookingProperties;
    private final SlotLockService slotLockService;
    private final OutboxService outboxService;

    public BookingService(
            BookingRepository bookingRepository,
            VenueClient venueClient,
            BookingProperties bookingProperties,
            SlotLockService slotLockService,
            OutboxService outboxService
    ) {
        this.bookingRepository = bookingRepository;
        this.venueClient = venueClient;
        this.bookingProperties = bookingProperties;
        this.slotLockService = slotLockService;
        this.outboxService = outboxService;
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request, UserPrincipal principal) {
        String lockToken = slotLockService.tryLock(request.slotId());
        try {
            VenueSlotResponse slot = venueClient.getSlot(request.slotId());

            if (!"AVAILABLE".equals(slot.status())) {
                throw new ApiException(HttpStatus.CONFLICT, "Slot is not available");
            }

            if (bookingRepository.existsBySlotIdAndStatusIn(slot.id(), ACTIVE_STATUSES)) {
                throw new ApiException(HttpStatus.CONFLICT, "Slot already has an active booking");
            }

            Booking booking = new Booking();
            booking.setUserId(principal.getId());
            booking.setShopId(slot.shopId());
            booking.setSlotId(slot.id());
            booking.setStatus(BookingStatus.PENDING);
            booking.setAmount(slot.price());
            booking.setExpiresAt(Instant.now().plus(bookingProperties.pendingTtlMinutes(), ChronoUnit.MINUTES));

            bookingRepository.save(booking);
            outboxService.enqueue(OutboxService.BOOKING_CREATED, booking);
            venueClient.invalidateShopSlotCache(slot.shopId());
            return BookingResponse.from(booking);
        } finally {
            slotLockService.unlock(request.slotId(), lockToken);
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse getById(UUID bookingId, UserPrincipal principal) {
        Booking booking = findAccessibleBooking(bookingId, principal);
        return BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listMine(UserPrincipal principal) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(BookingResponse::from)
                .toList();
    }

    /**
     * Saga: payment succeeded → confirm pending booking.
     */
    @Transactional
    public void confirmFromPayment(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            log.warn("Booking {} not found for PAYMENT_SUCCEEDED — absorbing", bookingId);
            return;
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.info("Booking {} already CONFIRMED (idempotent payment success)", bookingId);
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            log.warn("Skipping PAYMENT_SUCCEEDED for booking {} in status {}", bookingId, booking.getStatus());
            return;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        outboxService.enqueue(OutboxService.BOOKING_CONFIRMED, booking);
        slotLockService.forceRelease(booking.getSlotId());
        venueClient.invalidateShopSlotCache(booking.getShopId());
        log.info("Booking {} CONFIRMED from payment event", bookingId);
    }

    /**
     * Saga: payment failed → cancel pending booking and free slot.
     */
    @Transactional
    public void cancelFromPayment(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            log.warn("Booking {} not found for PAYMENT_FAILED — absorbing", bookingId);
            return;
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.info("Booking {} already CANCELLED (idempotent payment failure)", bookingId);
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CANCELLING) {
            log.warn("Skipping PAYMENT_FAILED for booking {} in status {}", bookingId, booking.getStatus());
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        outboxService.enqueue(OutboxService.BOOKING_CANCELLED, booking);
        slotLockService.forceRelease(booking.getSlotId());
        venueClient.invalidateShopSlotCache(booking.getShopId());
        log.info("Booking {} CANCELLED from payment event", bookingId);
    }

    /**
     * Auto-expire unpaid PENDING bookings past expires_at.
     */
    @Transactional
    public int expireOverduePendingBookings() {
        List<Booking> overdue = bookingRepository.findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING, Instant.now()
        );
        for (Booking booking : overdue) {
            booking.setStatus(BookingStatus.CANCELLED);
            outboxService.enqueue(OutboxService.BOOKING_CANCELLED, booking);
            slotLockService.forceRelease(booking.getSlotId());
            venueClient.invalidateShopSlotCache(booking.getShopId());
            log.info("Booking {} expired (PENDING past expires_at) → CANCELLED", booking.getId());
        }
        return overdue.size();
    }

    private Booking findAccessibleBooking(UUID bookingId, UserPrincipal principal) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (principal.getRole() == Role.ADMIN) {
            return booking;
        }
        if (!booking.getUserId().equals(principal.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not own this booking");
        }
        return booking;
    }
}
