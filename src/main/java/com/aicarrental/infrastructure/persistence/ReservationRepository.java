package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.domain.vehicle.Vehicle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT v
        FROM Vehicle v
        WHERE v.id = :vehicleId
          AND v.tenant.id = :tenantId
        """)
    Optional<Vehicle> findByIdAndTenantIdForUpdate(
            @Param("vehicleId") Long vehicleId,
            @Param("tenantId") Long tenantId
    );
    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Reservation r
        WHERE r.vehicle.id = :vehicleId
          AND r.status IN (
                         com.aicarrental.domain.reservation.ReservationStatus.PENDING_PAYMENT,
                         com.aicarrental.domain.reservation.ReservationStatus.DEPOSIT_PAID,
                         com.aicarrental.domain.reservation.ReservationStatus.CONFIRMED
                     )
          AND r.pickupDateTime < :returnDateTime
          AND r.returnDateTime > :pickupDateTime
        """)
    boolean existsOverlappingReservation(
            @Param("vehicleId") Long vehicleId,
            @Param("pickupDateTime") LocalDateTime pickupDateTime,
            @Param("returnDateTime") LocalDateTime returnDateTime
    );

}
