package com.aicarrental.infrastructure.kafka;

import com.aicarrental.application.notification.NotificationService;
import com.aicarrental.common.event.ReservationExpiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationExpiredNotificationConsumer {
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "reservation-expired",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeReservationExpired(
            ReservationExpiredEvent event
    ) {
        log.info(
                "ReservationExpiredEvent consumed. reservationId={}, tenantId={}",
                event.reservationId(),
                event.tenantId()
        );

        notificationService.createReservationExpiredNotification(event);
    }
}
