package com.aicarrental.infrastructure.ratelimit;

import com.aicarrental.common.exception.RateLimitExceededException;
import com.aicarrental.common.exception.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicAiRateLimiterTests {
    @Mock
    private StringRedisTemplate redisTemplate;

    private PublicAiRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new PublicAiRateLimiter(redisTemplate, 10, 60);
    }

    @Test
    void allowsRequestsWithinLimit() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString()
        )).thenReturn(10L);

        assertDoesNotThrow(() -> rateLimiter.checkAllowed("127.0.0.1"));
    }

    @Test
    void rejectsRequestsAboveLimit() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString()
        )).thenReturn(11L);

        RateLimitExceededException exception = assertThrows(
                RateLimitExceededException.class,
                () -> rateLimiter.checkAllowed("127.0.0.1")
        );

        assertEquals(60, exception.getRetryAfterSeconds());
    }

    @Test
    void returnsServiceUnavailableWhenRedisFails() {
        when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString()
        )).thenThrow(new IllegalStateException("redis unavailable"));

        assertThrows(
                ServiceUnavailableException.class,
                () -> rateLimiter.checkAllowed("127.0.0.1")
        );
    }
}
