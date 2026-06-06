package com.aicarrental.infrastructure.kafka;

import com.aicarrental.application.notification.NotificationService;
import com.aicarrental.common.event.AiPricingApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiPricingApprovedNotificationConsumer {
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "ai-pricing-approved",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeAiPricingApproved(
            AiPricingApprovedEvent event
    ) {
        log.info(
                "AiPricingApprovedEvent consumed. recommendationId={}, tenantId={}",
                event.recommendationId(),
                event.tenantId()
        );

        notificationService.createAiPricingApprovedNotification(event);
    }
}
