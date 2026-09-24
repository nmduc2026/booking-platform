package com.ndz.booking_service.client;

import com.ndz.booking_service.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;

@Component
public class VenueClient {

    private final RestClient venueRestClient;

    public VenueClient(RestClient venueRestClient) {
        this.venueRestClient = venueRestClient;
    }

    public VenueSlotResponse getSlot(UUID slotId) {
        try {
            VenueSlotResponse slot = venueRestClient.get()
                    .uri("/slots/{slotId}", slotId)
                    .retrieve()
                    .body(VenueSlotResponse.class);

            if (slot == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Empty response from venue-service");
            }
            return slot;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Slot not found");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to call venue-service");
        }
    }

    public void invalidateShopSlotCache(UUID shopId) {
        try {
            venueRestClient.delete()
                    .uri("/internal/shops/{shopId}/slot-cache", shopId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            // Cache invalidation must not fail the booking flow; TTL is the safety net.
        }
    }
}
