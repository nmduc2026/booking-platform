package com.ndz.venue_service.controller;

import com.ndz.venue_service.dto.CreateShopRequest;
import com.ndz.venue_service.dto.PageResponse;
import com.ndz.venue_service.dto.ShopResponse;
import com.ndz.venue_service.entity.ShopStatus;
import com.ndz.venue_service.security.UserPrincipal;
import com.ndz.venue_service.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminShopController {

    private final VenueService venueService;

    public AdminShopController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping("/shops")
    public PageResponse<ShopResponse> listShops(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ShopStatus status,
            @RequestParam(required = false) String q
    ) {
        return venueService.listShops(page, size, status, q);
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
