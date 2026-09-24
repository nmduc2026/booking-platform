package com.ndz.venue_service.controller;

import com.ndz.venue_service.dto.CreateResourceRequest;
import com.ndz.venue_service.dto.CreateTimeSlotRequest;
import com.ndz.venue_service.dto.ResourceResponse;
import com.ndz.venue_service.dto.ShopResponse;
import com.ndz.venue_service.dto.TimeSlotResponse;
import com.ndz.venue_service.security.UserPrincipal;
import com.ndz.venue_service.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/shops")
public class ShopController {

    private final VenueService venueService;

    public ShopController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public List<ShopResponse> listShops() {
        return venueService.listActiveShops();
    }

    @GetMapping("/{shopId}/slots")
    public List<TimeSlotResponse> listSlots(
            @PathVariable UUID shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return venueService.listAvailableSlots(shopId, date);
    }

    @PostMapping("/{shopId}/resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse createResource(
            @PathVariable UUID shopId,
            @Valid @RequestBody CreateResourceRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return venueService.createResource(shopId, request, principal);
    }

    @PostMapping("/{shopId}/slots")
    @ResponseStatus(HttpStatus.CREATED)
    public TimeSlotResponse createSlot(
            @PathVariable UUID shopId,
            @Valid @RequestBody CreateTimeSlotRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return venueService.createSlot(shopId, request, principal);
    }
}
