package com.aicarrental.infrastructure.kafka;
import com.aicarrental.common.event.RentalCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentalEventProducer {
    private static final String RENTAL_COMPLETED_TOPIC = "rental-completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>> publishRentalCompleted(
            RentalCompletedEvent event,
            String messageKey
    ) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(
                        RENTAL_COMPLETED_TOPIC,
                        messageKey,
                        event
                );

        future.whenComplete((result, exception) -> {
            if (exception == null) {
                log.info(
                        "RentalCompletedEvent successfully published for rentalId={} to topic={}, partition={}, offset={}",
                        event.rentalId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            } else {
                log.error(
                        "Failed to publish RentalCompletedEvent for rentalId={}. Reason={}",
                        event.rentalId(),
                        exception.getMessage(),
                        exception
                );
            }
        });

        return future;
    }
}
