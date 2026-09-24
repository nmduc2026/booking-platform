package com.ndz.realtime_service;

import com.ndz.realtime_service.messaging.BookingEventHandler;
import com.ndz.realtime_service.sse.ShopSseHub;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BookingEventHandlerTest {

    @Test
    void confirmedEventIsPublishedToShopHub() {
        AtomicReference<String> capturedEvent = new AtomicReference<>();
        AtomicReference<Object> capturedPayload = new AtomicReference<>();
        ShopSseHub hub = new ShopSseHub() {
            @Override
            public void publish(UUID shopId, String eventName, Object payload) {
                capturedEvent.set(eventName);
                capturedPayload.set(payload);
            }
        };

        UUID shopId = UUID.randomUUID();
        BookingEventHandler handler = new BookingEventHandler(new ObjectMapper(), hub);
        handler.handle("""
                {"eventType":"BOOKING_CONFIRMED","bookingId":"b1","shopId":"%s","slotId":"s1","status":"CONFIRMED","amount":10}
                """.formatted(shopId));

        assertThat(capturedEvent.get()).isEqualTo("booking");
        assertThat(capturedPayload.get().toString()).contains("BOOKING_CONFIRMED");
    }
}
