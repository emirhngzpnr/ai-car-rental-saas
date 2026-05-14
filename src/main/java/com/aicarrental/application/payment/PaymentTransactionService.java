package com.aicarrental.application.payment;

import com.aicarrental.api.payment.request.CreatePaymentRequest;
import com.aicarrental.api.payment.response.PaymentTransactionResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.event.PaymentCompletedEvent;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.payment.PaymentStatus;
import com.aicarrental.domain.payment.PaymentTransaction;
import com.aicarrental.domain.payment.PaymentType;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.kafka.PaymentEventProducer;
import com.aicarrental.infrastructure.persistence.PaymentTransactionRepository;
import com.aicarrental.infrastructure.persistence.ReservationRepository;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final TenantRepository tenantRepository;
    private final ReservationRepository reservationRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final CurrentUserService currentUserService;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentTransactionResponse createPayment(CreatePaymentRequest request) {

        return paymentTransactionRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(this::mapToResponse)
                .orElseGet(() -> createNewPayment(request));
    }

    private PaymentTransactionResponse createNewPayment(CreatePaymentRequest request) {

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Reservation reservation = null;

        if (request.reservationId() != null) {
            reservation = reservationRepository.findById(request.reservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

            if (!reservation.getTenant().getId().equals(tenant.getId())) {
                throw new BusinessException("Reservation does not belong to tenant");
            }
        }
        if (request.paymentType() == PaymentType.DEPOSIT_PAYMENT && reservation == null) {
            throw new BusinessException("Deposit payment must be linked to a reservation");
        }

        if (request.paymentType() == PaymentType.DEPOSIT_PAYMENT) {
            boolean depositAlreadyPaid =
                    paymentTransactionRepository.existsByReservation_IdAndPaymentTypeAndPaymentStatus(
                            reservation.getId(),
                            PaymentType.DEPOSIT_PAYMENT,
                            PaymentStatus.SUCCESS
                    );

            if (depositAlreadyPaid) {
                throw new BusinessException("Deposit payment already completed for this reservation");
            }
        }
        LocalDateTime now = LocalDateTime.now();

        PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                .tenant(tenant)
                .reservation(reservation)
                .paymentType(request.paymentType())
                .paymentStatus(PaymentStatus.SUCCESS)
                .amount(request.amount())
                .currency("TRY")
                .providerTransactionId("MOCK-" + System.currentTimeMillis())
                .idempotencyKey(request.idempotencyKey())
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentTransaction saved = paymentTransactionRepository.save(paymentTransaction);

        if (saved.getPaymentType() == PaymentType.DEPOSIT_PAYMENT
                && saved.getPaymentStatus() == PaymentStatus.SUCCESS
                && saved.getReservation() != null) {

            Reservation paidReservation = saved.getReservation();

            paidReservation.setStatus(ReservationStatus.DEPOSIT_PAID);
            paidReservation.setUpdatedAt(LocalDateTime.now());

            reservationRepository.save(paidReservation);
        }

        User currentUser = currentUserService.getCurrentUser();

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                saved.getTenant() != null ? saved.getTenant().getId() : null,
                AuditAction.PAYMENT_COMPLETED,
                "PaymentTransaction",
                saved.getId(),
                "Payment completed. Type: " + saved.getPaymentType()
                        + ", Amount: " + saved.getAmount()
                        + " " + saved.getCurrency()
        ));
        paymentEventProducer.publishPaymentCompleted(
                new PaymentCompletedEvent(
                        saved.getId(),
                        saved.getReservation() != null ? saved.getReservation().getId() : null,
                        saved.getTenant() != null ? saved.getTenant().getId() : null,
                        saved.getAmount(),
                        saved.getCurrency(),
                        saved.getPaymentType().name(),
                        LocalDateTime.now()
                )
        );

        return mapToResponse(saved);
    }

    private PaymentTransactionResponse mapToResponse(PaymentTransaction paymentTransaction) {
        return new PaymentTransactionResponse(
                paymentTransaction.getId(),
                paymentTransaction.getTenant() != null ? paymentTransaction.getTenant().getId() : null,
                paymentTransaction.getReservation() != null ? paymentTransaction.getReservation().getId() : null,
                paymentTransaction.getRental() != null ? paymentTransaction.getRental().getId() : null,
                paymentTransaction.getPaymentType(),
                paymentTransaction.getPaymentStatus(),
                paymentTransaction.getAmount(),
                paymentTransaction.getCurrency(),
                paymentTransaction.getProviderTransactionId(),
                paymentTransaction.getIdempotencyKey(),
                paymentTransaction.getCreatedAt(),
                paymentTransaction.getUpdatedAt()
        );
    }
}
