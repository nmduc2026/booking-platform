package com.ndz.payment_service.outbox;

public interface PaymentEventPublisher {
    void publish(String eventType, String payload);
}
