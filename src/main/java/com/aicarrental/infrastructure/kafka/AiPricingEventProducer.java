package com.aicarrental.infrastructure.kafka;

import com.aicarrental.common.event.AiPricingApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiPricingEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> publishAiPricingApproved(
            AiPricingApprovedEvent event,
            String messageKey
    ) {
        return kafkaTemplate.send(
                "ai-pricing-approved",
                messageKey,
                event
        );
    }
}
