package com.ndz.booking_service.controller;

import com.ndz.booking_service.dto.BookingResponse;
import com.ndz.booking_service.dto.CreateBookingRequest;
import com.ndz.booking_service.security.UserPrincipal;
import com.ndz.booking_service.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            @Valid @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return bookingService.create(request, principal);
    }

    @GetMapping("/me")
    public List<BookingResponse> listMine(@AuthenticationPrincipal UserPrincipal principal) {
        return bookingService.listMine(principal);
    }

    @GetMapping("/shop/{shopId}")
    public List<BookingResponse> listByShop(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return bookingService.listByShop(shopId, principal);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getById(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return bookingService.getById(bookingId, principal);
    }
}
