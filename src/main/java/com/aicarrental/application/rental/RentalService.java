package com.aicarrental.application.rental;

import com.aicarrental.api.rental.request.CompleteRentalRequest;
import com.aicarrental.api.rental.request.StartRentalRequest;
import com.aicarrental.api.rental.response.RentalResponse;
import com.aicarrental.application.outbox.OutboxMessageService;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.event.RentalCompletedEvent;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.outbox.OutboxEventType;
import com.aicarrental.domain.rental.Rental;
import com.aicarrental.domain.rental.RentalStatus;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.domain.vehicle.VehicleStatus;
import com.aicarrental.infrastructure.persistence.RentalRepository;
import com.aicarrental.infrastructure.persistence.ReservationRepository;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalService {
    private final RentalRepository rentalRepository;
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;
    private final AuditEventPublisher auditEventPublisher;
    private final OutboxMessageService outboxMessageService;

    public RentalResponse startRental(StartRentalRequest request) {

        User currentUser = currentUserService.getCurrentUser();

        Reservation reservation =
                findReservationByIdWithTenantIsolation(request.reservationId());

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException("Only confirmed reservations can start rental");
        }

        if (rentalRepository.existsByReservation_IdAndActiveTrue(reservation.getId())) {
            throw new BusinessException("Rental already exists for this reservation");
        }

        Vehicle vehicle = reservation.getVehicle();

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BusinessException("Vehicle is not available");
        }

        vehicle.setStatus(VehicleStatus.RENTED);
        vehicle.setUpdatedAt(LocalDateTime.now());

        vehicleRepository.save(vehicle);
        long rentalDays = Math.max(
                1,
                Duration.between(
                        reservation.getPickupDateTime(),
                        reservation.getReturnDateTime()
                ).toDays()
        );

        int allowedKm = Math.toIntExact(
                reservation.getDailyKmLimitSnapshot() * rentalDays
        );
        Rental rental = Rental.builder()
                .reservation(reservation)
                .vehicle(vehicle)
                .tenant(reservation.getTenant())

                .actualPickupDateTime(request.actualPickupDateTime())

                .startMileage(request.startMileage())

                .allowedKm(allowedKm)

                .status(RentalStatus.ACTIVE)
                .active(true)

                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Rental savedRental = rentalRepository.save(rental);

        reservation.setStatus(ReservationStatus.CONVERTED_TO_RENTAL);
        reservation.setUpdatedAt(LocalDateTime.now());

        reservationRepository.save(reservation);

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                savedRental.getTenant() != null ? savedRental.getTenant().getId() : null,
                AuditAction.RENTAL_STARTED,
                "Rental",
                savedRental.getId(),
                "Rental started for vehicle: " + vehicle.getPlateNumber()
        ));

        return mapToResponse(savedRental);
    }

    public List<RentalResponse> getAllRentals() {

        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isSuperAdmin(currentUser)) {
            return rentalRepository.findByActiveTrue()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        Long tenantId = currentUserService.getCurrentTenantId();

        return rentalRepository.findByTenant_IdAndActiveTrue(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public RentalResponse getRentalById(Long rentalId) {

        Rental rental = findRentalByIdWithTenantIsolation(rentalId);

        return mapToResponse(rental);
    }
    public RentalResponse completeRental(Long rentalId, CompleteRentalRequest request){
        User currentUser = currentUserService.getCurrentUser();

        Rental rental = findRentalByIdWithTenantIsolation(rentalId);

        if (rental.getStatus() != RentalStatus.ACTIVE) {
            throw new BusinessException("Rental is not active");
        }

        if (request.endMileage() < rental.getStartMileage()) {
            throw new BusinessException("End mileage cannot be lower than start mileage");
        }

        int usedKm = request.endMileage() - rental.getStartMileage();

        int extraKm = Math.max(0, usedKm - rental.getAllowedKm());

        BigDecimal extraKmFee = rental.getReservation()
                .getExtraKmPricePerKmSnapshot()
                .multiply(BigDecimal.valueOf(extraKm))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal finalRentalPrice = rental.getReservation()
                .getEstimatedRentalPrice()
                .add(extraKmFee)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal deposit = rental.getReservation().getDepositAmount();

        BigDecimal depositDeduction = extraKmFee.min(deposit)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal refundAmount = deposit.subtract(depositDeduction)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        rental.setActualReturnDateTime(request.actualReturnDateTime());
        rental.setEndMileage(request.endMileage());
        rental.setUsedKm(usedKm);
        rental.setExtraKm(extraKm);
        rental.setExtraKmFee(extraKmFee);
        rental.setFinalRentalPrice(finalRentalPrice);
        rental.setDepositDeduction(depositDeduction);
        rental.setRefundAmount(refundAmount);
        rental.setStatus(RentalStatus.COMPLETED);
        rental.setUpdatedAt(LocalDateTime.now());

        Vehicle vehicle = rental.getVehicle();
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle.setCurrentMileage(request.endMileage());
        vehicle.setUpdatedAt(LocalDateTime.now());

        vehicleRepository.save(vehicle);

        Rental completedRental = rentalRepository.save(rental);
        outboxMessageService.createOutboxMessage(
                "rental-completed",
                String.valueOf(completedRental.getId()),
                OutboxEventType.RENTAL_COMPLETED,
                new RentalCompletedEvent(
                        completedRental.getId(),
                        completedRental.getReservation() != null
                                ? completedRental.getReservation().getId()
                                : null,
                        completedRental.getTenant() != null
                                ? completedRental.getTenant().getId()
                                : null,
                        LocalDateTime.now()
                )
        );
        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                completedRental.getTenant() != null ? completedRental.getTenant().getId() : null,
                AuditAction.RENTAL_COMPLETED,
                "Rental",
                completedRental.getId(),
                "Rental completed for vehicle: " + vehicle.getPlateNumber()
        ));

        return mapToResponse(completedRental);
    }
    private Rental findRentalByIdWithTenantIsolation(Long rentalId) {

        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isSuperAdmin(currentUser)) {
            return rentalRepository.findByIdAndActiveTrue(rentalId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Rental not found"));
        }

        Long tenantId = currentUserService.getCurrentTenantId();

        return rentalRepository
                .findByIdAndTenant_IdAndActiveTrue(rentalId, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Rental not found"));
    }

    private Reservation findReservationByIdWithTenantIsolation(Long reservationId) {

        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isSuperAdmin(currentUser)) {
            return reservationRepository.findByIdAndActiveTrue(reservationId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Reservation not found"));
        }

        Long tenantId = currentUserService.getCurrentTenantId();

        return reservationRepository
                .findByIdAndTenant_IdAndActiveTrue(reservationId, tenantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Reservation not found"));
    }

    private RentalResponse mapToResponse(Rental rental) {

        return new RentalResponse(
                rental.getId(),

                rental.getReservation() != null
                        ? rental.getReservation().getId()
                        : null,

                rental.getTenant() != null
                        ? rental.getTenant().getId()
                        : null,

                rental.getTenant() != null
                        ? rental.getTenant().getCompanyName()
                        : null,

                rental.getVehicle() != null
                        ? rental.getVehicle().getId()
                        : null,

                rental.getVehicle() != null
                        ? rental.getVehicle().getBrand()
                        : null,

                rental.getVehicle() != null
                        ? rental.getVehicle().getModel()
                        : null,

                rental.getVehicle() != null
                        ? rental.getVehicle().getPlateNumber()
                        : null,

                rental.getActualPickupDateTime(),

                rental.getActualReturnDateTime(),

                rental.getStartMileage(),

                rental.getEndMileage(),

                rental.getUsedKm(),

                rental.getAllowedKm(),

                rental.getExtraKm(),

                rental.getExtraKmFee(),

                rental.getFinalRentalPrice(),

                rental.getDepositDeduction(),

                rental.getRefundAmount(),

                rental.getStatus().name(),

                rental.getActive(),

                rental.getCreatedAt(),

                rental.getUpdatedAt()
        );
    }
}
