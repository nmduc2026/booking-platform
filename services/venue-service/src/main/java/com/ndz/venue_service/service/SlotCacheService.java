package com.ndz.venue_service.service;

import com.ndz.venue_service.dto.TimeSlotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SlotCacheService {

    private static final Logger log = LoggerFactory.getLogger(SlotCacheService.class);
    private static final Duration TTL = Duration.ofSeconds(45);
    private static final TypeReference<List<TimeSlotResponse>> SLOT_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SlotCacheService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public List<TimeSlotResponse> get(UUID shopId, LocalDate date) {
        String cached = redis.opsForValue().get(key(shopId, date));
        if (cached == null) {
            log.info("slot-cache MISS shopId={} date={}", shopId, date);
            return null;
        }
        log.info("slot-cache HIT shopId={} date={}", shopId, date);
        return objectMapper.readValue(cached, SLOT_LIST_TYPE);
    }

    public void put(UUID shopId, LocalDate date, List<TimeSlotResponse> slots) {
        redis.opsForValue().set(key(shopId, date), objectMapper.writeValueAsString(slots), TTL);
    }

    public void invalidateShop(UUID shopId) {
        Set<String> keys = redis.keys("slots:available:" + shopId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
            log.info("slot-cache INVALIDATE shopId={} keys={}", shopId, keys.size());
        }
    }

    private static String key(UUID shopId, LocalDate date) {
        return "slots:available:" + shopId + ":" + date;
    }
}
