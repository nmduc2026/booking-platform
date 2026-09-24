package com.ndz.venue_service.controller;

import com.ndz.venue_service.service.VenueService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class InternalCacheController {

    private final VenueService venueService;

    public InternalCacheController(VenueService venueService) {
        this.venueService = venueService;
    }

    @DeleteMapping("/shops/{shopId}/slot-cache")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invalidateSlotCache(@PathVariable UUID shopId) {
        venueService.invalidateShopSlotCache(shopId);
    }
}
