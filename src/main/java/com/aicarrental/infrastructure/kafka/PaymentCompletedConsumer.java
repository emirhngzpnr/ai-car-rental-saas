package com.aicarrental.infrastructure.kafka;

import com.aicarrental.application.notification.NotificationService;
import com.aicarrental.application.outbox.KafkaEventProcessingService;
import com.aicarrental.common.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {
    private final NotificationService notificationService;
    private final KafkaEventProcessingService eventProcessingService;

    @KafkaListener(
            topics = "payment-completed",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentCompleted(
            PaymentCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey
    ) {

        log.info(
                "PaymentCompletedEvent consumed. paymentId={}, reservationId={}, tenantId={}, amount={} {}, paymentType={}, completedAt={}",
                event.paymentId(),
                event.reservationId(),
                event.tenantId(),
                event.amount(),
                event.currency(),
                event.paymentType(),
                event.completedAt()
        );
        eventProcessingService.processOnce(
                "payment-completed-notification",
                topic,
                eventKey(messageKey, event.paymentId()),
                () -> notificationService.createPaymentCompletedNotification(event)
        );

        // future:
        // notificationService.sendPaymentCompletedMail(...)
        // invoiceService.prepareReceipt(...)
        // analyticsService.updatePaymentMetrics(...)
    }

    private String eventKey(String messageKey, Long aggregateId) {
        return messageKey != null ? messageKey : String.valueOf(aggregateId);
    }
}
