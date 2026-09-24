package com.ndz.realtime_service.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ShopSseHub {

    private static final Logger log = LoggerFactory.getLogger(ShopSseHub.class);

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByShop = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID shopId) {
        SseEmitter emitter = new SseEmitter(0L);
        emittersByShop.computeIfAbsent(shopId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(shopId, emitter));
        emitter.onTimeout(() -> remove(shopId, emitter));
        emitter.onError(ex -> remove(shopId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("shopId", shopId.toString())));
        } catch (IOException ex) {
            remove(shopId, emitter);
        }
        return emitter;
    }

    public void publish(UUID shopId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByShop.get(shopId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception ex) {
                log.debug("Removing dead SSE emitter for shop {}: {}", shopId, ex.getMessage());
                remove(shopId, emitter);
            }
        }
    }

    private void remove(UUID shopId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByShop.get(shopId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByShop.remove(shopId, emitters);
            }
        }
    }
}
