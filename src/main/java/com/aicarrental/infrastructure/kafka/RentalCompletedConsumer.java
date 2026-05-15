package com.aicarrental.infrastructure.kafka;
import com.aicarrental.application.invoice.InvoiceService;
import com.aicarrental.common.event.RentalCompletedEvent;
import com.aicarrental.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RentalCompletedConsumer {
    private final InvoiceService invoiceService;

    @KafkaListener(
            topics = "rental-completed",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeRentalCompleted(RentalCompletedEvent event) {

        log.info(
                "RentalCompletedEvent consumed. rentalId={}, reservationId={}, tenantId={}, completedAt={}",
                event.rentalId(),
                event.reservationId(),
                event.tenantId(),
                event.completedAt()
        );

        try {
            invoiceService.createRentalCompletionInvoice(event.rentalId());
        } catch (BusinessException exception) {
            log.warn(
                    "Invoice creation skipped for rentalId={}. Reason={}",
                    event.rentalId(),
                    exception.getMessage()
            );
        }
    }
}
