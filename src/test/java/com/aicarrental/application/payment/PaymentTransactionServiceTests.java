package com.aicarrental.application.payment;

import com.aicarrental.api.payment.request.CreatePaymentRequest;
import com.aicarrental.application.outbox.OutboxMessageService;
import com.aicarrental.application.report.ReportCacheInvalidator;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.payment.PaymentType;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.PaymentTransactionRepository;
import com.aicarrental.infrastructure.persistence.ReservationRepository;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTests {
    @Mock PaymentTransactionRepository paymentTransactionRepository;
    @Mock TenantRepository tenantRepository;
    @Mock ReservationRepository reservationRepository;
    @Mock AuditEventPublisher auditEventPublisher;
    @Mock CurrentUserService currentUserService;
    @Mock OutboxMessageService outboxMessageService;
    @Mock ReportCacheInvalidator reportCacheInvalidator;

    private PaymentTransactionService service;

    @BeforeEach
    void setUp() {
        service = new PaymentTransactionService(
                paymentTransactionRepository,
                tenantRepository,
                reservationRepository,
                auditEventPublisher,
                currentUserService,
                outboxMessageService,
                reportCacheInvalidator
        );
    }

    @Test
    void tenantAdminCannotCreatePaymentForAnotherTenant() {
        Tenant tenant = Tenant.builder().id(1L).companyName("Tenant").slug("tenant").build();
        User user = User.builder()
                .id(10L)
                .email("admin@tenant.com")
                .role(Role.TENANT_ADMIN)
                .tenant(tenant)
                .active(true)
                .build();

        when(paymentTransactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(currentUserService.isSuperAdmin(user)).thenReturn(false);
        when(currentUserService.getCurrentTenantId()).thenReturn(1L);

        CreatePaymentRequest request = new CreatePaymentRequest(
                2L,
                100L,
                null,
                PaymentType.DEPOSIT_PAYMENT,
                BigDecimal.valueOf(100),
                "key-1"
        );

        assertThrows(BusinessException.class, () -> service.createPayment(request));

        verify(tenantRepository, never()).findById(2L);
        verify(paymentTransactionRepository, never()).saveAndFlush(any());
    }
}
