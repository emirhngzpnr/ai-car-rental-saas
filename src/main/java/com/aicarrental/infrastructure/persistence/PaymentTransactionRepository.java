package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.payment.PaymentStatus;
import com.aicarrental.domain.payment.PaymentTransaction;
import com.aicarrental.domain.payment.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
