package com.aicarrental.infrastructure.kafka;

import com.aicarrental.common.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {
    private static final String PAYMENT_COMPLETED_TOPIC = "payment-completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentCompleted(PaymentCompletedEvent event) {
        kafkaTemplate.send(
                PAYMENT_COMPLETED_TOPIC,
                String.valueOf(event.paymentId()),
                event
        ).whenComplete((result, exception) -> {
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
    }
}
