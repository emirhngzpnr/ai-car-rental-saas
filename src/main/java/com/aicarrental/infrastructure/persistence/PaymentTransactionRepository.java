package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.payment.PaymentStatus;
import com.aicarrental.domain.payment.PaymentTransaction;
import com.aicarrental.domain.payment.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
    boolean existsByReservation_IdAndPaymentTypeAndPaymentStatus(
            Long reservationId,
            PaymentType paymentType,
            PaymentStatus paymentStatus
    );
    boolean existsByRental_IdAndPaymentTypeAndPaymentStatus(
            Long rentalId,
            PaymentType paymentType,
            PaymentStatus paymentStatus
    );
    Long countByTenant_IdAndPaymentStatus(Long tenantId, PaymentStatus paymentStatus);

    Long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM PaymentTransaction p
        WHERE p.tenant.id = :tenantId
          AND p.paymentType = :paymentType
          AND p.paymentStatus = :paymentStatus
        """)
    BigDecimal sumAmountByTenantAndTypeAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("paymentType") PaymentType paymentType,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM PaymentTransaction p
        WHERE p.paymentType = :paymentType
          AND p.paymentStatus = :paymentStatus
        """)
    BigDecimal sumAmountByTypeAndStatus(
            @Param("paymentType") PaymentType paymentType,
            @Param("paymentStatus") PaymentStatus paymentStatus
    );
    @Query(value = """
        SELECT COALESCE(SUM(p.amount), 0)
        FROM rental.payment_transactions p
        WHERE p.payment_type = 'REFUND'
          AND p.payment_status = 'REFUNDED'
          AND p.tenant_id = :tenantId
          AND EXTRACT(YEAR FROM p.created_at) = :year
          AND EXTRACT(MONTH FROM p.created_at) = :month
        """, nativeQuery = true)
    BigDecimal sumMonthlyRefundAmountByTenant(
            @Param("tenantId") Long tenantId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
        SELECT COALESCE(SUM(p.amount), 0)
        FROM rental.payment_transactions p
        WHERE p.payment_type = 'REFUND'
          AND p.payment_status = 'REFUNDED'
          AND EXTRACT(YEAR FROM p.created_at) = :year
          AND EXTRACT(MONTH FROM p.created_at) = :month
        """, nativeQuery = true)
    BigDecimal sumMonthlyRefundAmount(
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
