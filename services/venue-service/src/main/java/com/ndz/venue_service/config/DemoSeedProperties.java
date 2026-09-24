package com.ndz.venue_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record DemoSeedProperties(boolean enabled) {
}
