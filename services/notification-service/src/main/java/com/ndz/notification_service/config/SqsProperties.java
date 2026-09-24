package com.ndz.notification_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sqs")
public record SqsProperties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bookingEventsQueue,
        boolean consumerEnabled,
        long consumerPollIntervalMs,
        int consumerBatchSize,
        int consumerWaitTimeSeconds
) {
}
