package com.aicarrental.application.payment;
import com.aicarrental.application.outbox.OutboxMessageService;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.event.RefundCompletedEvent;
import com.aicarrental.domain.outbox.OutboxEventType;
import com.aicarrental.domain.payment.PaymentStatus;
import com.aicarrental.domain.payment.PaymentTransaction;
import com.aicarrental.domain.payment.PaymentType;
import com.aicarrental.domain.rental.Rental;
import com.aicarrental.infrastructure.persistence.PaymentTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundService {
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OutboxMessageService outboxMessageService;
    private final AuditEventPublisher auditEventPublisher;

    public void createRefundForCompletedRental(Rental rental) {

        if (rental.getRefundAmount() == null ||
                rental.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        boolean refundAlreadyExists =
                paymentTransactionRepository.existsByRental_IdAndPaymentTypeAndPaymentStatus(
                        rental.getId(),
                        PaymentType.REFUND,
                        PaymentStatus.REFUNDED
                );

        if (refundAlreadyExists) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        PaymentTransaction refundPayment =
                PaymentTransaction.builder()
                        .tenant(rental.getTenant())
                        .reservation(rental.getReservation())
                        .rental(rental)
                        .paymentType(PaymentType.REFUND)
                        .paymentStatus(PaymentStatus.REFUNDED)
                        .amount(rental.getRefundAmount())
                        .currency("TRY")
                        .providerTransactionId("MOCK-REFUND-" + System.currentTimeMillis())
                        .idempotencyKey("refund-rental-" + rental.getId())
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

        PaymentTransaction savedRefund =
                paymentTransactionRepository.save(refundPayment);

        outboxMessageService.createOutboxMessage(
                "refund-completed",
                String.valueOf(savedRefund.getId()),
                OutboxEventType.REFUND_COMPLETED,
                new RefundCompletedEvent(
                        savedRefund.getId(),
                        rental.getId(),
                        rental.getReservation() != null
                                ? rental.getReservation().getId()
                                : null,
                        rental.getTenant() != null
                                ? rental.getTenant().getId()
                                : null,
                        rental.getRefundAmount(),
                        savedRefund.getCurrency(),
                        now
                )
        );

        auditEventPublisher.publish(new AuditEvent(
                null,
                "SYSTEM",
                "SYSTEM",
                rental.getTenant() != null ? rental.getTenant().getId() : null,
                AuditAction.REFUND_COMPLETED,
                "PaymentTransaction",
                savedRefund.getId(),
                "Refund completed for rentalId: " + rental.getId()
                        + ", Amount: " + rental.getRefundAmount()
                        + " " + savedRefund.getCurrency()
        ));
    }
}
