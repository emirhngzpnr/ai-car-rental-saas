package com.aicarrental.infrastructure.kafka;
import com.aicarrental.application.notification.NotificationService;
import com.aicarrental.application.outbox.KafkaEventProcessingService;
import com.aicarrental.common.event.RefundCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundCompletedConsumer {
    private final NotificationService notificationService;
    private final KafkaEventProcessingService eventProcessingService;

    @KafkaListener(
            topics = "refund-completed",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeRefundCompleted(
            RefundCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey
    ) {

        log.info(
                "RefundCompletedEvent consumed. paymentId={}, rentalId={}, reservationId={}, tenantId={}, refundAmount={} {}, completedAt={}",
                event.paymentId(),
                event.rentalId(),
                event.reservationId(),
                event.tenantId(),
                event.refundAmount(),
                event.currency(),
                event.completedAt()
        );

        eventProcessingService.processOnce(
                "refund-completed-notification",
                topic,
                messageKey != null ? messageKey : String.valueOf(event.paymentId()),
                () -> notificationService.createRefundCompletedNotification(event)
        );
    }
}
