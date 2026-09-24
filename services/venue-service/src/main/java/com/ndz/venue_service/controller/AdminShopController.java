package com.ndz.venue_service.controller;

import com.ndz.venue_service.dto.CreateShopRequest;
import com.ndz.venue_service.dto.ShopResponse;
import com.ndz.venue_service.security.UserPrincipal;
import com.ndz.venue_service.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminShopController {

    private final VenueService venueService;

    public AdminShopController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping("/shops")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopResponse createShop(
            @Valid @RequestBody CreateShopRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return venueService.createShop(request, principal.getId());
    }
}
