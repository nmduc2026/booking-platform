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
import com.ndz.booking_service.repository.BookingRepository;
import com.ndz.booking_service.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.CANCELLING
    );

    private final BookingRepository bookingRepository;
    private final VenueClient venueClient;
    private final BookingProperties bookingProperties;

    public BookingService(
            BookingRepository bookingRepository,
            VenueClient venueClient,
            BookingProperties bookingProperties
    ) {
        this.bookingRepository = bookingRepository;
        this.venueClient = venueClient;
        this.bookingProperties = bookingProperties;
    }

    @Transactional
    public BookingResponse create(CreateBookingRequest request, UserPrincipal principal) {
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
        return BookingResponse.from(booking);
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

    @Transactional
    public BookingResponse confirm(UUID bookingId, UserPrincipal principal) {
        Booking booking = findAccessibleBooking(bookingId, principal);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PENDING bookings can be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse cancel(UUID bookingId, UserPrincipal principal) {
        Booking booking = findAccessibleBooking(bookingId, principal);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.CANCELLING) {
            throw new ApiException(HttpStatus.CONFLICT, "Booking is already cancelling");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(booking);
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
