package com.aicarrental.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentCompletedConsumer {
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

        // future:
        // notificationService.sendPaymentCompletedMail(...)
        // invoiceService.prepareReceipt(...)
        // analyticsService.updatePaymentMetrics(...)
    }
}
