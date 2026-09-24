package com.ndz.realtime_service.messaging;

import com.ndz.realtime_service.sse.ShopSseHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingEventHandler {

    public static final String BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String BOOKING_CANCELLED = "BOOKING_CANCELLED";

    private static final Logger log = LoggerFactory.getLogger(BookingEventHandler.class);

    private final ObjectMapper objectMapper;
    private final ShopSseHub shopSseHub;

    public BookingEventHandler(ObjectMapper objectMapper, ShopSseHub shopSseHub) {
        this.objectMapper = objectMapper;
        this.shopSseHub = shopSseHub;
    }

    public void handle(String messageBody) {
        JsonNode root = objectMapper.readTree(messageBody);
        String eventType = text(root, "eventType");
        String shopIdRaw = text(root, "shopId");
        if (eventType == null || shopIdRaw == null) {
            throw new IllegalArgumentException("booking event missing eventType/shopId");
        }

        if (!BOOKING_CONFIRMED.equals(eventType) && !BOOKING_CANCELLED.equals(eventType)) {
            log.debug("Ignoring booking event type {}", eventType);
            return;
        }

        UUID shopId = UUID.fromString(shopIdRaw);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("bookingId", text(root, "bookingId"));
        payload.put("shopId", shopIdRaw);
        payload.put("slotId", text(root, "slotId"));
        payload.put("status", text(root, "status"));
        payload.put("amount", root.get("amount") == null ? null : root.get("amount").asString());

        shopSseHub.publish(shopId, "booking", payload);
        log.info("Pushed SSE {} for shop {}", eventType, shopId);
    }

    private static String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asString();
    }
}
