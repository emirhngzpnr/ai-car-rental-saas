package com.aicarrental.infrastructure.kafka;

import com.aicarrental.application.notification.NotificationService;
import com.aicarrental.application.outbox.KafkaEventProcessingService;
import com.aicarrental.common.event.ReservationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCreatedNotificationConsumer {
    private final NotificationService notificationService;
    private final KafkaEventProcessingService eventProcessingService;

    @KafkaListener(
            topics = "reservation-created",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeReservationCreated(
            ReservationCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey
    ) {
        log.info(
                "ReservationCreatedEvent consumed. reservationId={}, tenantId={}",
                event.reservationId(),
                event.tenantId()
        );

        eventProcessingService.processOnce(
                "reservation-created-notification",
                topic,
                messageKey != null ? messageKey : String.valueOf(event.reservationId()),
                () -> notificationService.createReservationCreatedNotification(event)
        );
    }

}
