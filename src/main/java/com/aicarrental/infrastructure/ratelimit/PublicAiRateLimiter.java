package com.aicarrental.infrastructure.ratelimit;

import com.aicarrental.common.exception.RateLimitExceededException;
import com.aicarrental.common.exception.ServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
@Slf4j
public class PublicAiRateLimiter {
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final long windowSeconds;

    public PublicAiRateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${ai.marketplace.rate-limit.max-requests:10}") int maxRequests,
            @Value("${ai.marketplace.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
    }

    public void checkAllowed(String clientAddress) {
        String key = "rate-limit:public-ai:" + hash(clientAddress);
        Long requestCount;

        try {
            requestCount = redisTemplate.execute(
                    INCREMENT_SCRIPT,
                    List.of(key),
                    Long.toString(windowSeconds)
            );
        } catch (Exception exception) {
            log.warn("Public AI rate limit storage is unavailable: {}", exception.getClass().getSimpleName());
            throw new ServiceUnavailableException(
                    "AI vehicle search is temporarily unavailable. Manual filters are still available."
            );
        }

        if (requestCount == null) {
            throw new ServiceUnavailableException(
                    "AI vehicle search is temporarily unavailable. Manual filters are still available."
            );
        }
        if (requestCount > maxRequests) {
            throw new RateLimitExceededException(
                    "Too many AI search requests. Please try again shortly.",
                    windowSeconds
            );
        }
    }

    private String hash(String clientAddress) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(clientAddress.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
