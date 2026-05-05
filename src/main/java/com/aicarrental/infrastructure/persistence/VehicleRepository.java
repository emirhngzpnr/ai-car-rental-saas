package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.vehicle.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    boolean existsByPlateNumberAndTenant_Id(String plateNumber, Long tenantId);

    boolean existsByPlateNumberAndTenant_IdAndIdNot(String plateNumber, Long tenantId, Long id);

    List<Vehicle> findByActiveTrue();

    List<Vehicle> findByTenant_IdAndActiveTrue(Long tenantId);

    Optional<Vehicle> findByIdAndActiveTrue(Long id);

    Optional<Vehicle> findByIdAndTenant_IdAndActiveTrue(Long id, Long tenantId);
}
