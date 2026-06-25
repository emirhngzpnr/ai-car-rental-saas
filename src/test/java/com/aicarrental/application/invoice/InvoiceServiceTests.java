package com.aicarrental.application.invoice;

import com.aicarrental.application.report.ReportCacheInvalidator;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.invoice.Invoice;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.InvoiceRepository;
import com.aicarrental.infrastructure.persistence.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTests {
    @Mock InvoiceRepository invoiceRepository;
    @Mock RentalRepository rentalRepository;
    @Mock InvoiceNumberGeneratorService invoiceNumberGeneratorService;
    @Mock AuditEventPublisher auditEventPublisher;
    @Mock ReportCacheInvalidator reportCacheInvalidator;
    @Mock CurrentUserService currentUserService;

    private InvoiceService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceService(
                invoiceRepository,
                rentalRepository,
                invoiceNumberGeneratorService,
                auditEventPublisher,
                reportCacheInvalidator,
                currentUserService
        );
    }

    @Test
    void tenantUserCanOnlyResolveInvoiceFromOwnTenant() {
        Tenant tenant = Tenant.builder().id(7L).companyName("Tenant").build();
        User user = User.builder()
                .id(2L)
                .email("admin@tenant.test")
                .role(Role.TENANT_ADMIN)
                .tenant(tenant)
                .build();
        Invoice invoice = Invoice.builder().id(12L).tenant(tenant).build();

        when(currentUserService.isSuperAdmin()).thenReturn(false);
        when(currentUserService.getCurrentTenantId()).thenReturn(7L);
        when(invoiceRepository.findByIdAndTenant_Id(12L, 7L))
                .thenReturn(Optional.of(invoice));

        assertSame(invoice, service.getAccessibleInvoice(12L));
        verify(invoiceRepository, never()).findById(12L);
    }

    @Test
    void crossTenantInvoiceIsReturnedAsNotFound() {
        when(currentUserService.isSuperAdmin()).thenReturn(false);
        when(currentUserService.getCurrentTenantId()).thenReturn(7L);
        when(invoiceRepository.findByIdAndTenant_Id(99L, 7L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getAccessibleInvoice(99L)
        );

        verify(invoiceRepository, never()).findById(99L);
    }

    @Test
    void superAdminCanResolveInvoiceGlobally() {
        Invoice invoice = Invoice.builder().id(12L).build();
        when(currentUserService.isSuperAdmin()).thenReturn(true);
        when(invoiceRepository.findById(12L)).thenReturn(Optional.of(invoice));

        assertSame(invoice, service.getAccessibleInvoice(12L));
        verify(invoiceRepository, never()).findByIdAndTenant_Id(anyLong(), anyLong());
    }
}
