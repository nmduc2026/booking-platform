package com.ndz.booking_service.outbox;

public interface BookingEventPublisher {

    void publish(String eventType, String payload);
}
