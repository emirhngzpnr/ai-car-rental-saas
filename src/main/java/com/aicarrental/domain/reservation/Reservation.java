package com.aicarrental.domain.reservation;


import com.aicarrental.domain.insurance.InsurancePackage;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.domain.vehicle.Vehicle;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations", schema = "rental")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private String customerFullName;

    @Column(nullable = false)
    private String customerPhone;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String customerIdentityNumber;

    @Column(nullable = false)
    private LocalDateTime pickupDateTime;

    @Column(nullable = false)
    private LocalDateTime returnDateTime;

    @Column(nullable = false)
    private BigDecimal dailyPriceSnapshot;

    @Column(nullable = false)
    private Integer dailyKmLimitSnapshot;

    @Column(nullable = false)
    private BigDecimal extraKmPricePerKmSnapshot;

    @Column(nullable = false)
    private BigDecimal depositAmount;

    @Column(nullable = false)
    private BigDecimal estimatedRentalPrice;

    @Column(nullable = false)
    private BigDecimal totalEstimatedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false)
    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_package_id")
    private InsurancePackage insurancePackage;

    @Column(name = "insurance_package_name_snapshot", length = 100)
    private String insurancePackageNameSnapshot;

    @Column(name = "insurance_package_type_snapshot", length = 50)
    private String insurancePackageTypeSnapshot;

    @Column(name = "insurance_daily_price_snapshot", precision = 12, scale = 2)
    private BigDecimal insuranceDailyPriceSnapshot;

    @Column(name = "insurance_total_price_snapshot", precision = 12, scale = 2)
    private BigDecimal insuranceTotalPriceSnapshot;
}
