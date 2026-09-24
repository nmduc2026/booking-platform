package com.ndz.booking_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.venue-service")
public record VenueServiceProperties(String baseUrl) {
}
