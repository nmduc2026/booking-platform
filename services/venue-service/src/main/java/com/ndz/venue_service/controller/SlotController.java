package com.ndz.venue_service.controller;

import com.ndz.venue_service.dto.TimeSlotResponse;
import com.ndz.venue_service.service.VenueService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/slots")
public class SlotController {

    private final VenueService venueService;

    public SlotController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping("/{slotId}")
    public TimeSlotResponse getSlot(@PathVariable UUID slotId) {
        return venueService.getSlot(slotId);
    }
}
