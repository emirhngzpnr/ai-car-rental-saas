package com.aicarrental.domain.rental;

import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.domain.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rentals", schema = "rental")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rental {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private LocalDateTime actualPickupDateTime;

    private LocalDateTime actualReturnDateTime;

    @Column(nullable = false)
    private Integer startMileage;

    private Integer endMileage;

    private Integer usedKm;

    private Integer allowedKm;

    private Integer extraKm;

    private BigDecimal extraKmFee;

    private BigDecimal finalRentalPrice;

    private BigDecimal depositDeduction;

    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus status;

    @Column(nullable = false)
    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
