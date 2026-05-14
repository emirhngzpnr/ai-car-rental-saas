package com.aicarrental.infrastructure.kafka;

import com.aicarrental.common.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    private static final String PAYMENT_COMPLETED_TOPIC = "payment-completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<SendResult<String, Object>>
    publishPaymentCompleted(
            PaymentCompletedEvent event,
            String messageKey
    ) {

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(
                        PAYMENT_COMPLETED_TOPIC,
                        messageKey,
                        event
                );

        future.whenComplete((result, exception) -> {

            if (exception == null) {

                log.info(
                        "PaymentCompletedEvent successfully published for paymentId={} to topic={}, partition={}, offset={}",
                        event.paymentId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );

            } else {

                log.error(
                        "Failed to publish PaymentCompletedEvent for paymentId={}. Reason={}",
                        event.paymentId(),
                        exception.getMessage(),
                        exception
                );
            }
        });

        return future;
    }
}
