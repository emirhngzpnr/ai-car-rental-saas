package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.invoice.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByRental_Id(Long rentalId);

    boolean existsByRental_Id(Long rentalId);
}
