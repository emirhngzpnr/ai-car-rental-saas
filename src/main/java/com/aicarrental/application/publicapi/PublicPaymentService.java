package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.request.PublicDepositPaymentRequest;
import com.aicarrental.api.publicapi.response.PublicDepositPaymentResponse;
import com.aicarrental.application.outbox.OutboxMessageService;
import com.aicarrental.application.payment.provider.PaymentProvider;
import com.aicarrental.application.payment.provider.PaymentProviderResult;
import com.aicarrental.application.report.ReportCacheInvalidator;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.event.PaymentCompletedEvent;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.outbox.OutboxEventType;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.domain.payment.PaymentStatus;
import com.aicarrental.domain.payment.PaymentTransaction;
import com.aicarrental.domain.payment.PaymentType;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.infrastructure.persistence.PaymentTransactionRepository;
import com.aicarrental.infrastructure.persistence.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicPaymentService {
    private final PublicReservationService publicReservationService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReservationRepository reservationRepository;
    private final OutboxMessageService outboxMessageService;
    private final AuditEventPublisher auditEventPublisher;
    private final ReportCacheInvalidator reportCacheInvalidator;
    private final PaymentProvider paymentProvider;

    public PublicDepositPaymentResponse payDeposit(
            String tenantSlug,
            String reservationCode,
            PublicDepositPaymentRequest request
    ) {
        Reservation reservation = publicReservationService.findPublicReservationForPaymentForUpdate(
                tenantSlug,
                reservationCode,
                request.email()
        );

        return completeDeposit(reservation, request.idempotencyKey());
    }

    public PublicDepositPaymentResponse payDepositForCustomer(
            CustomerAccount customer,
            String reservationCode,
            String idempotencyKey
    ) {
        Reservation reservation = reservationRepository
                .findCustomerReservationForPaymentForUpdate(reservationCode, customer.getId())
                .orElseThrow(() -> new com.aicarrental.common.exception.ResourceNotFoundException("Reservation not found"));

        return completeDeposit(reservation, idempotencyKey);
    }

    private PublicDepositPaymentResponse completeDeposit(Reservation reservation, String idempotencyKey) {

        if (reservation.getStatus() != ReservationStatus.PENDING_PAYMENT) {
            throw new BusinessException("Deposit payment can only be completed for pending payment reservations");
        }

        if (paymentTransactionRepository.existsByReservation_IdAndPaymentTypeAndPaymentStatus(
                reservation.getId(),
                PaymentType.DEPOSIT_PAYMENT,
                PaymentStatus.SUCCESS
        )) {
            throw new BusinessException("Deposit payment already completed for this reservation");
        }

        paymentTransactionRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(existing -> {
                    throw new BusinessException("Idempotency key already used");
                });

        LocalDateTime now = LocalDateTime.now();
        PaymentProviderResult providerResult = paymentProvider.chargeDeposit(
                reservation.getDepositAmount(),
                "TRY",
                idempotencyKey
        );

        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .tenant(reservation.getTenant())
                .reservation(reservation)
                .paymentType(PaymentType.DEPOSIT_PAYMENT)
                .paymentStatus(PaymentStatus.SUCCESS)
                .amount(reservation.getDepositAmount())
                .currency("TRY")
                .providerTransactionId(providerResult.providerTransactionId())
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentTransaction savedPayment;
        try {
            savedPayment = paymentTransactionRepository.saveAndFlush(paymentTransaction);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("Deposit payment already completed for this reservation");
        }

        reservation.setStatus(ReservationStatus.DEPOSIT_PAID);
        reservation.setUpdatedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        publishPaymentCompleted(savedPayment);
        publishAudit(savedPayment);
        reportCacheInvalidator.evictAfterCommit();

        return new PublicDepositPaymentResponse(
                reservation.getReservationCode(),
                reservation.getStatus().name(),
                savedPayment.getPaymentStatus().name(),
                savedPayment.getAmount(),
                savedPayment.getCurrency(),
                savedPayment.getCreatedAt()
        );
    }

    private void publishPaymentCompleted(PaymentTransaction paymentTransaction) {
        outboxMessageService.createOutboxMessage(
                "payment-completed",
                paymentTransaction.getId().toString(),
                OutboxEventType.PAYMENT_COMPLETED,
                new PaymentCompletedEvent(
                        paymentTransaction.getId(),
                        paymentTransaction.getReservation() != null ? paymentTransaction.getReservation().getId() : null,
                        paymentTransaction.getTenant() != null ? paymentTransaction.getTenant().getId() : null,
                        paymentTransaction.getAmount(),
                        paymentTransaction.getCurrency(),
                        paymentTransaction.getPaymentType().name(),
                        LocalDateTime.now()
                )
        );
    }

    private void publishAudit(PaymentTransaction paymentTransaction) {
        auditEventPublisher.publish(new AuditEvent(
                null,
                paymentTransaction.getReservation().getCustomerEmail(),
                "PUBLIC_CUSTOMER",
                paymentTransaction.getTenant().getId(),
                AuditAction.PAYMENT_COMPLETED,
                "PaymentTransaction",
                paymentTransaction.getId(),
                "Public deposit payment completed for reservation: "
                        + paymentTransaction.getReservation().getReservationCode()
        ));
    }
}
