package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.invoice.Invoice;
import com.aicarrental.domain.invoice.InvoiceStatus;
import com.aicarrental.infrastructure.persistence.projection.MonthlyRevenueProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByRental_Id(Long rentalId);

    boolean existsByRental_Id(Long rentalId);
    @Query("""
        SELECT COALESCE(SUM(i.totalAmount), 0)
        FROM Invoice i
        WHERE i.tenant.id = :tenantId
          AND i.status = :status
        """)
    BigDecimal sumTotalAmountByTenantAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") InvoiceStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(i.totalAmount), 0)
        FROM Invoice i
        WHERE i.status = :status
        """)
    BigDecimal sumTotalAmountByStatus(
            @Param("status") InvoiceStatus status
    );
    @Query(value = """
        SELECT
            TO_CHAR(i.issued_at, 'YYYY-MM') AS month,
            COALESCE(SUM(i.total_amount), 0) AS totalRevenue
        FROM rental.invoices i
        WHERE i.status = 'ISSUED'
          AND i.tenant_id = :tenantId
        GROUP BY TO_CHAR(i.issued_at, 'YYYY-MM')
        ORDER BY month
        """, nativeQuery = true)
    List<MonthlyRevenueProjection> getMonthlyRevenueByTenant(
            @Param("tenantId") Long tenantId
    );

    @Query(value = """
        SELECT
            TO_CHAR(i.issued_at, 'YYYY-MM') AS month,
            COALESCE(SUM(i.total_amount), 0) AS totalRevenue
        FROM rental.invoices i
        WHERE i.status = 'ISSUED'
        GROUP BY TO_CHAR(i.issued_at, 'YYYY-MM')
        ORDER BY month
        """, nativeQuery = true)
    List<MonthlyRevenueProjection> getMonthlyRevenue();
    @Query(value = """
        SELECT COALESCE(SUM(i.total_amount), 0)
        FROM rental.invoices i
        WHERE i.status = 'ISSUED'
          AND i.tenant_id = :tenantId
          AND EXTRACT(YEAR FROM i.issued_at) = :year
          AND EXTRACT(MONTH FROM i.issued_at) = :month
        """, nativeQuery = true)
    BigDecimal sumMonthlyInvoiceAmountByTenant(
            @Param("tenantId") Long tenantId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
        SELECT COALESCE(SUM(i.total_amount), 0)
        FROM rental.invoices i
        WHERE i.status = 'ISSUED'
          AND EXTRACT(YEAR FROM i.issued_at) = :year
          AND EXTRACT(MONTH FROM i.issued_at) = :month
        """, nativeQuery = true)
    BigDecimal sumMonthlyInvoiceAmount(
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM rental.invoices i
        WHERE i.status = 'ISSUED'
          AND i.tenant_id = :tenantId
          AND EXTRACT(YEAR FROM i.issued_at) = :year
          AND EXTRACT(MONTH FROM i.issued_at) = :month
        """, nativeQuery = true)
    Long countMonthlyIssuedInvoicesByTenant(
            @Param("tenantId") Long tenantId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM rental.invoices i
        WHERE i.status = 'ISSUED'
          AND EXTRACT(YEAR FROM i.issued_at) = :year
          AND EXTRACT(MONTH FROM i.issued_at) = :month
        """, nativeQuery = true)
    Long countMonthlyIssuedInvoices(
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
