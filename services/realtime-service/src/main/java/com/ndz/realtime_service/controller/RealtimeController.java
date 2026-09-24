package com.ndz.realtime_service.controller;

import com.ndz.realtime_service.sse.ShopSseHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/realtime")
public class RealtimeController {

    private final ShopSseHub shopSseHub;

    public RealtimeController(ShopSseHub shopSseHub) {
        this.shopSseHub = shopSseHub;
    }

    @GetMapping(value = "/shops/{shopId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable UUID shopId) {
        return shopSseHub.subscribe(shopId);
    }
}
