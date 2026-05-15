package com.aicarrental.domain.invoice;

import com.aicarrental.domain.rental.Rental;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.tenant.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "invoices",
        schema = "rental",
        indexes = {
                @Index(name = "idx_invoices_tenant_id", columnList = "tenant_id"),
                @Index(name = "idx_invoices_invoice_number", columnList = "invoice_number"),
                @Index(name = "idx_invoices_status", columnList = "status"),
                @Index(name = "idx_invoices_issued_at", columnList = "issued_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_invoices_invoice_number",
                        columnNames = "invoice_number"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, length = 50, updatable = false)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", updatable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", updatable = false)
    private Rental rental;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50, updatable = false)
    private InvoiceType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InvoiceStatus status;

    @Column(name = "customer_full_name_snapshot", nullable = false, length = 150, updatable = false)
    private String customerFullNameSnapshot;

    @Column(name = "customer_email_snapshot", nullable = false, length = 150, updatable = false)
    private String customerEmailSnapshot;

    @Column(name = "customer_phone_snapshot", length = 50, updatable = false)
    private String customerPhoneSnapshot;

    @Column(name = "customer_identity_number_snapshot", length = 50, updatable = false)
    private String customerIdentityNumberSnapshot;

    @Column(name = "vehicle_plate_number_snapshot", nullable = false, length = 50, updatable = false)
    private String vehiclePlateNumberSnapshot;

    @Column(name = "vehicle_brand_snapshot", length = 100, updatable = false)
    private String vehicleBrandSnapshot;

    @Column(name = "vehicle_model_snapshot", length = 100, updatable = false)
    private String vehicleModelSnapshot;

    @Column(name = "rental_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal rentalAmount;

    @Column(name = "extra_km_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal extraKmAmount;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal depositAmount;

    @Column(name = "deposit_deduction_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal depositDeductionAmount;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal refundAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = InvoiceStatus.ISSUED;
        }

        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "TRY";
        }

        if (this.issuedAt == null) {
            this.issuedAt = LocalDateTime.now();
        }
    }
}