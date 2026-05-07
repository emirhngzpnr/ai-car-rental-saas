package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.vehicle.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
