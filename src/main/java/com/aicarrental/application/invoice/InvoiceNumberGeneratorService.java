package com.aicarrental.application.invoice;
import com.aicarrental.domain.invoice.InvoiceCounter;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.InvoiceCounterRepository;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Year;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceNumberGeneratorService {
    private final InvoiceCounterRepository invoiceCounterRepository;
    private final TenantRepository tenantRepository;

    public String generateInvoiceNumber(Long tenantId) {

        int year = Year.now().getValue();

        InvoiceCounter counter =
                invoiceCounterRepository.findByTenant_IdAndInvoiceYear(
                                tenantId,
                                year
                        )
                        .orElseGet(() -> createInitialCounter(tenantId, year));

        Long nextNumber = counter.getLastNumber() + 1;
        counter.setLastNumber(nextNumber);

        invoiceCounterRepository.save(counter);

        return String.format(
                "INV-%d-%d-%06d",
                tenantId,
                year,
                nextNumber
        );
    }

    private InvoiceCounter createInitialCounter(Long tenantId, int year) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        return invoiceCounterRepository.save(
                InvoiceCounter.builder()
                        .tenant(tenant)
                        .invoiceYear(year)
                        .lastNumber(0L)
                        .build()
        );
    }
}
