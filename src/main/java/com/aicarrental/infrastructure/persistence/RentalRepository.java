package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.rental.Rental;
import com.aicarrental.domain.rental.RentalStatus;
import com.aicarrental.infrastructure.persistence.projection.TopVehicleProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long>{
    boolean existsByReservation_IdAndActiveTrue(Long reservationId);

    boolean existsByVehicle_IdAndStatusAndActiveTrue(Long vehicleId, RentalStatus status);

    List<Rental> findByActiveTrue();

    List<Rental> findByTenant_IdAndActiveTrue(Long tenantId);

    Optional<Rental> findByIdAndActiveTrue(Long id);

    Optional<Rental> findByIdAndTenant_IdAndActiveTrue(Long id, Long tenantId);

    Long countByTenant_IdAndStatus(Long tenantId, RentalStatus status);

    Long countByStatus(RentalStatus status);

    @Query("""
        SELECT COALESCE(SUM(r.finalRentalPrice), 0)
        FROM Rental r
        WHERE r.tenant.id = :tenantId
          AND r.status = :status
        """)
    BigDecimal sumFinalRentalPriceByTenantAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") RentalStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(r.finalRentalPrice), 0)
        FROM Rental r
        WHERE r.status = :status
        """)
    BigDecimal sumFinalRentalPriceByStatus(
            @Param("status") RentalStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(r.extraKmFee), 0)
        FROM Rental r
        WHERE r.tenant.id = :tenantId
          AND r.status = :status
        """)
    BigDecimal sumExtraKmFeeByTenantAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") RentalStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(r.extraKmFee), 0)
        FROM Rental r
        WHERE r.status = :status
        """)
    BigDecimal sumExtraKmFeeByStatus(
            @Param("status") RentalStatus status
    );
    @Query(value = """
        SELECT COUNT(*)
        FROM rental.rentals r
        WHERE r.status = 'COMPLETED'
          AND r.tenant_id = :tenantId
          AND EXTRACT(YEAR FROM r.updated_at) = :year
          AND EXTRACT(MONTH FROM r.updated_at) = :month
        """, nativeQuery = true)
    Long countMonthlyCompletedRentalsByTenant(
            @Param("tenantId") Long tenantId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM rental.rentals r
        WHERE r.status = 'COMPLETED'
          AND EXTRACT(YEAR FROM r.updated_at) = :year
          AND EXTRACT(MONTH FROM r.updated_at) = :month
        """, nativeQuery = true)
    Long countMonthlyCompletedRentals(
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
        SELECT COALESCE(SUM(r.extra_km_fee), 0)
        FROM rental.rentals r
        WHERE r.status = 'COMPLETED'
          AND r.tenant_id = :tenantId
          AND EXTRACT(YEAR FROM r.updated_at) = :year
          AND EXTRACT(MONTH FROM r.updated_at) = :month
        """, nativeQuery = true)
    BigDecimal sumMonthlyExtraKmRevenueByTenant(
            @Param("tenantId") Long tenantId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    @Query(value = """
        SELECT COALESCE(SUM(r.extra_km_fee), 0)
        FROM rental.rentals r
        WHERE r.status = 'COMPLETED'
          AND EXTRACT(YEAR FROM r.updated_at) = :year
          AND EXTRACT(MONTH FROM r.updated_at) = :month
        """, nativeQuery = true)
    BigDecimal sumMonthlyExtraKmRevenue(
            @Param("year") Integer year,
            @Param("month") Integer month
    );
    @Query(value = """
        SELECT
            v.id AS vehicleId,
            v.plate_number AS plateNumber,
            v.brand AS brand,
            v.model AS model,
            COUNT(r.id) AS rentalCount,
            COALESCE(SUM(r.final_rental_price), 0) AS totalRevenue
        FROM rental.rentals r
        JOIN rental.vehicles v ON v.id = r.vehicle_id
        WHERE r.status = 'COMPLETED'
          AND r.tenant_id = :tenantId
        GROUP BY v.id, v.plate_number, v.brand, v.model
        ORDER BY rentalCount DESC, totalRevenue DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<TopVehicleProjection> findTopVehiclesByTenant(
            @Param("tenantId") Long tenantId,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT
            v.id AS vehicleId,
            v.plate_number AS plateNumber,
            v.brand AS brand,
            v.model AS model,
            COUNT(r.id) AS rentalCount,
            COALESCE(SUM(r.final_rental_price), 0) AS totalRevenue
        FROM rental.rentals r
        JOIN rental.vehicles v ON v.id = r.vehicle_id
        WHERE r.status = 'COMPLETED'
        GROUP BY v.id, v.plate_number, v.brand, v.model
        ORDER BY rentalCount DESC, totalRevenue DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<TopVehicleProjection> findTopVehicles(
            @Param("limit") int limit
    );
}
