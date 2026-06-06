package com.aicarrental.infrastructure.kafka;

import com.aicarrental.application.notification.NotificationService;
import com.aicarrental.common.event.ReservationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCreatedNotificationConsumer {
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "reservation-created",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeReservationCreated(
            ReservationCreatedEvent event
    ) {
        log.info(
                "ReservationCreatedEvent consumed. reservationId={}, tenantId={}",
                event.reservationId(),
                event.tenantId()
        );

        notificationService.createReservationCreatedNotification(event);
    }

}
