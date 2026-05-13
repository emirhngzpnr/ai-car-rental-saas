package com.aicarrental.domain.payment;

import com.aicarrental.domain.rental.Rental;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.tenant.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(
        name = "payment_transactions",
        schema = "rental",
        indexes = {
                @Index(name = "idx_payment_transactions_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_payment_transactions_reservation_id", columnList = "reservation_id"),
                @Index(name = "idx_payment_transactions_rental_id", columnList = "rental_id"),
                @Index(name = "idx_payment_transactions_idempotency_key", columnList = "idempotency_key")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_transactions_idempotency_key",
                        columnNames = "idempotency_key"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id")
    private Rental rental;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 50)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "provider_transaction_id", length = 100)
    private String providerTransactionId;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();

        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.PENDING;
        }

        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "TRY";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}













