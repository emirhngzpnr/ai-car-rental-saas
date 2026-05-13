package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByActiveTrue();

    List<Reservation> findByTenant_IdAndActiveTrue(Long tenantId);

    Optional<Reservation> findByIdAndActiveTrue(Long id);

    Optional<Reservation> findByIdAndTenant_IdAndActiveTrue(Long id, Long tenantId);

    boolean existsByVehicle_IdAndActiveTrueAndStatusInAndPickupDateTimeLessThanAndReturnDateTimeGreaterThan(
            Long vehicleId,
            List<ReservationStatus> statuses,
            LocalDateTime returnDateTime,
            LocalDateTime pickupDateTime
    );
    List<Reservation> findByStatusAndActiveTrueAndCreatedAtBefore(
            ReservationStatus status,
            LocalDateTime createdBefore
    );
}
