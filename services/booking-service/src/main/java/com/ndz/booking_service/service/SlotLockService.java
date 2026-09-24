package com.ndz.booking_service.service;

import com.ndz.booking_service.exception.ApiException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class SlotLockService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            else
              return 0
            end
            """,
            Long.class
    );

    private final StringRedisTemplate redis;

    public SlotLockService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String tryLock(UUID slotId) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redis.opsForValue().setIfAbsent(key(slotId), token, LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Slot is being held by another request, please retry"
            );
        }
        return token;
    }

    public void unlock(UUID slotId, String token) {
        redis.execute(RELEASE_SCRIPT, List.of(key(slotId)), token);
    }

    /**
     * Best-effort release for saga / expiry paths where the create lock token is gone.
     */
    public void forceRelease(UUID slotId) {
        redis.delete(key(slotId));
    }

    private static String key(UUID slotId) {
        return "lock:slot:" + slotId;
    }
}
