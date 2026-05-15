package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.invoice.InvoiceCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface InvoiceCounterRepository extends JpaRepository<InvoiceCounter, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvoiceCounter> findByTenant_IdAndInvoiceYear(
            Long tenantId,
            Integer invoiceYear
    );

}
