package com.ndz.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sqs")
public record SqsProperties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String paymentEventsQueue
) {
}
