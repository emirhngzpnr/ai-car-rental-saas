package com.aicarrental.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration configuration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(15))
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(new StringRedisSerializer())
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(serializer)
                        )
                        .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(configuration)
                .build();
    }
}
