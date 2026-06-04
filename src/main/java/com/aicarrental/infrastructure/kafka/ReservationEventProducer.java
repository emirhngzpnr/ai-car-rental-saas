package com.aicarrental.infrastructure.kafka;

import com.aicarrental.common.event.ReservationCreatedEvent;
import com.aicarrental.common.event.ReservationExpiredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> publishReservationCreated(
            ReservationCreatedEvent event,
            String messageKey
    ) {
        return kafkaTemplate.send(
                "reservation-created",
                messageKey,
                event
        );
    }

    public CompletableFuture<SendResult<String, Object>> publishReservationExpired(
            ReservationExpiredEvent event,
            String messageKey
    ) {
        return kafkaTemplate.send(
                "reservation-expired",
                messageKey,
                event
        );
    }
}
