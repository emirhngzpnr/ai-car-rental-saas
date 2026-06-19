package com.aicarrental.domain.vehicle;

import com.aicarrental.domain.tenant.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "vehicles",
        schema = "rental",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehicle_tenant_plate",
                        columnNames = {"tenant_id", "plate_number"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "plate_number", nullable = false)
    private String plateNumber;

    @Column(name = "production_year")
    private Integer productionYear;

    @Column(name = "current_mileage", nullable = false)
    private Integer currentMileage;


    @Column(name = "daily_km_limit", nullable = false)
    private Integer dailyKmLimit;

    @Column(name = "extra_km_price_per_km", nullable = false)
    private BigDecimal extraKmPricePerKm;


    @Column(name = "daily_price", nullable = false)
    private BigDecimal dailyPrice;

    @Enumerated(EnumType.STRING)
    private VehicleCategory category;

    @Enumerated(EnumType.STRING)
    private TransmissionType transmission;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type")
    private FuelType fuelType;

    @Column(name = "seat_count")
    private Integer seatCount;

    private String location;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(nullable = false)
    private Boolean active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
