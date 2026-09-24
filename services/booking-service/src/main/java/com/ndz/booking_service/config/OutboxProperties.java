package com.ndz.booking_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(
        boolean relayEnabled,
        long pollIntervalMs,
        int batchSize
) {
}
