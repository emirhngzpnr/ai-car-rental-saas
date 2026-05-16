package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.domain.vehicle.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
/**
 * Repository interface for managing vehicle persistence operations.
 *
 * This repository is designed for a multi-tenant SaaS architecture
 * with soft delete support and tenant isolation rules.
 *
 * Key responsibilities:
 * - Prevent duplicate plate numbers
 * - Enforce tenant-aware data access
 * - Exclude soft-deleted vehicles from active queries
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    /**
     * Checks whether a vehicle plate number already exists globally.
     *
     * Business rule:
     * A physical vehicle plate should be unique across the entire system.
     *
     * Example:
     * 34ABC123 cannot belong to multiple companies simultaneously.
     */
    boolean existsByPlateNumber(String plateNumber);

    /**
     * Checks whether another vehicle with the same plate number exists,
     * excluding the current vehicle being updated.
     *
     * Used during update operations to avoid false duplicate conflicts.
     */
    boolean existsByPlateNumberAndIdNot(String plateNumber, Long id);

    /**
     * Returns all active vehicles in the system.
     *
     * Soft-deleted vehicles (active = false) are excluded.
     */
    List<Vehicle> findByActiveTrue();

    /**
     * Returns all active vehicles belonging to a specific tenant.
     *
     * Critical for tenant isolation:
     * each company can only access its own vehicles.
     */
    List<Vehicle> findByTenant_IdAndActiveTrue(Long tenantId);

    /**
     * Returns an active vehicle by its id.
     *
     * Soft-deleted vehicles are excluded automatically.
     */
    Optional<Vehicle> findByIdAndActiveTrue(Long id);

    /**
     * Returns an active vehicle by id and tenant id.
     *
     * This query provides the highest level of tenant isolation
     * and prevents cross-tenant data access vulnerabilities.
     */
    Optional<Vehicle> findByIdAndTenant_IdAndActiveTrue(Long id, Long tenantId);

    @Query("""
        SELECT v
        FROM Vehicle v
        WHERE v.active = true
         AND v.status = :availableStatus
          AND v.id NOT IN (
              SELECT r.vehicle.id
              FROM Reservation r
              WHERE r.active = true
                AND r.status IN :blockingStatuses
                AND r.pickupDateTime < :returnDateTime
                AND r.returnDateTime > :pickupDateTime
          )
        """)
    List<Vehicle> findAvailableVehicles(
            @Param("pickupDateTime") LocalDateTime pickupDateTime,
            @Param("returnDateTime") LocalDateTime returnDateTime,
            @Param("blockingStatuses") List<ReservationStatus> blockingStatuses,
            @Param("availableStatus") VehicleStatus availableStatus
    );

    @Query("""
        SELECT v
        FROM Vehicle v
        WHERE v.active = true
          AND v.tenant.id = :tenantId
         AND v.status = :availableStatus
          AND v.id NOT IN (
              SELECT r.vehicle.id
              FROM Reservation r
              WHERE r.active = true
                AND r.status IN :blockingStatuses
                AND r.pickupDateTime < :returnDateTime
                AND r.returnDateTime > :pickupDateTime
          )
        """)
    List<Vehicle> findAvailableVehiclesByTenant(
            @Param("tenantId") Long tenantId,
            @Param("pickupDateTime") LocalDateTime pickupDateTime,
            @Param("returnDateTime") LocalDateTime returnDateTime,
            @Param("blockingStatuses") List<ReservationStatus> blockingStatuses,
            @Param("availableStatus") VehicleStatus availableStatus
    );
    Long countByTenant_IdAndStatusAndActiveTrue(Long tenantId, VehicleStatus status);

    Long countByStatusAndActiveTrue(VehicleStatus status);
}
