package com.ndz.payment_service.stripe;

import java.util.Map;

public interface StripeGateway {

    CreatedPaymentIntent createPaymentIntent(long amountCents, String currency, Map<String, String> metadata);

    WebhookEvent parseWebhook(String payload, String signatureHeader);

    record CreatedPaymentIntent(String id, String clientSecret, String status) {
    }

    record WebhookEvent(String type, String paymentIntentId) {
    }
}
