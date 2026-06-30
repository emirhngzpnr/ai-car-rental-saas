package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.request.PublicDepositPaymentRequest;
import com.aicarrental.application.outbox.OutboxMessageService;
import com.aicarrental.application.payment.provider.PaymentProvider;
import com.aicarrental.application.report.ReportCacheInvalidator;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.PaymentTransactionRepository;
import com.aicarrental.infrastructure.persistence.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicPaymentServiceTests {
    @Mock PublicReservationService publicReservationService;
    @Mock PaymentTransactionRepository paymentTransactionRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock OutboxMessageService outboxMessageService;
    @Mock AuditEventPublisher auditEventPublisher;
    @Mock ReportCacheInvalidator reportCacheInvalidator;
    @Mock PaymentProvider paymentProvider;

    private PublicPaymentService service;

    @BeforeEach
    void setUp() {
        service = new PublicPaymentService(
                publicReservationService,
                paymentTransactionRepository,
                reservationRepository,
                outboxMessageService,
                auditEventPublisher,
                reportCacheInvalidator,
                paymentProvider
        );
    }

    @Test
    void guestDepositUsesLockedReservationLookupAndRejectsExistingSuccessfulDeposit() {
        Reservation reservation = Reservation.builder()
                .id(11L)
                .tenant(Tenant.builder().id(3L).slug("fastcar").build())
                .reservationCode("RNT-2026-000011")
                .customerEmail("customer@example.com")
                .status(ReservationStatus.PENDING_PAYMENT)
                .depositAmount(BigDecimal.valueOf(300))
                .build();

        when(publicReservationService.findPublicReservationForPaymentForUpdate(
                "fastcar",
                "RNT-2026-000011",
                "customer@example.com"
        )).thenReturn(reservation);
        when(paymentTransactionRepository.existsByReservation_IdAndPaymentTypeAndPaymentStatus(
                org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.payDeposit(
                "fastcar",
                "RNT-2026-000011",
                new PublicDepositPaymentRequest("customer@example.com", "deposit-key")
        ));

        verify(publicReservationService).findPublicReservationForPaymentForUpdate(
                "fastcar",
                "RNT-2026-000011",
                "customer@example.com"
        );
        verify(paymentProvider, never()).chargeDeposit(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
