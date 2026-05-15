package com.aicarrental.infrastructure.kafka;

import com.aicarrental.application.notification.NotificationService;
import com.aicarrental.common.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {
    private final NotificationService notificationService;
    @KafkaListener(
            topics = "payment-completed",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumePaymentCompleted(PaymentCompletedEvent event) {

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
        notificationService.createPaymentCompletedNotification(event);

        // future:
        // notificationService.sendPaymentCompletedMail(...)
        // invoiceService.prepareReceipt(...)
        // analyticsService.updatePaymentMetrics(...)
    }
}
