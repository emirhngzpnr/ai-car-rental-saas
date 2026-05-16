package com.aicarrental.infrastructure.kafka;
import com.aicarrental.application.notification.NotificationService;
import com.aicarrental.common.event.RefundCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundCompletedConsumer {
    private final NotificationService notificationService;
    @KafkaListener(
            topics = "refund-completed",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeRefundCompleted(RefundCompletedEvent event) {

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

        notificationService.createRefundCompletedNotification(event);
    }
}
