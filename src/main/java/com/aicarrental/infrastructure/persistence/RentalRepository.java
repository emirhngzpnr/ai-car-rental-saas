package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.rental.Rental;
import com.aicarrental.domain.rental.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long>{
    boolean existsByReservation_IdAndActiveTrue(Long reservationId);

    boolean existsByVehicle_IdAndStatusAndActiveTrue(Long vehicleId, RentalStatus status);

    List<Rental> findByActiveTrue();

    List<Rental> findByTenant_IdAndActiveTrue(Long tenantId);

    Optional<Rental> findByIdAndActiveTrue(Long id);

    Optional<Rental> findByIdAndTenant_IdAndActiveTrue(Long id, Long tenantId);
}
