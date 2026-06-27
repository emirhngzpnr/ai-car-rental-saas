package com.aicarrental.infrastructure.kafka;
import com.aicarrental.application.invoice.InvoiceEmailService;
import com.aicarrental.application.invoice.InvoiceService;
import com.aicarrental.application.outbox.KafkaEventProcessingService;
import com.aicarrental.common.event.RentalCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentalCompletedConsumer {
    private final InvoiceService invoiceService;
    private final InvoiceEmailService invoiceEmailService;
    private final KafkaEventProcessingService eventProcessingService;

    @KafkaListener(
            topics = "rental-completed",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeRentalCompleted(
            RentalCompletedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String messageKey
    ) {

        log.info(
                "RentalCompletedEvent consumed. rentalId={}, reservationId={}, tenantId={}, completedAt={}",
                event.rentalId(),
                event.reservationId(),
                event.tenantId(),
                event.completedAt()
        );

        eventProcessingService.processOnce(
                "rental-completed-invoice",
                topic,
                messageKey != null ? messageKey : String.valueOf(event.rentalId()),
                () -> {
                    var invoice = invoiceService.createRentalCompletionInvoiceIfAbsent(event.rentalId());
                    invoiceEmailService.sendRentalCompletionInvoice(invoice);
                }
        );
    }
}
