package com.aicarrental.application.reservation;

import com.aicarrental.api.reservation.request.CreateReservationRequest;
import com.aicarrental.api.reservation.response.ReservationResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.domain.vehicle.VehicleStatus;
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
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final VehicleRepository vehicleRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final CurrentUserService currentUserService;




    private boolean isSuperAdmin(User user) {
        return user.getRole() == Role.SUPER_ADMIN;
    }

    // Create Reservation
    public ReservationResponse createReservation(CreateReservationRequest request) {

        User currentUser = currentUserService.getCurrentUser();

        Vehicle vehicle = findVehicleByIdWithTenantIsolation(request.vehicleId());

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BusinessException("Vehicle is not available");
        }

        if (request.returnDateTime().isBefore(request.pickupDateTime())) {
            throw new BusinessException("Return date cannot be before pickup date");
        }

        boolean hasConflict =
                reservationRepository
                        .existsByVehicle_IdAndActiveTrueAndStatusInAndPickupDateTimeLessThanAndReturnDateTimeGreaterThan(
                                vehicle.getId(),
                                List.of(
                                        ReservationStatus.PENDING_PAYMENT,
                                        ReservationStatus.CONFIRMED
                                ),
                                request.returnDateTime(),
                                request.pickupDateTime()
                        );

        if (hasConflict) {
            throw new BusinessException("Vehicle is already reserved for selected dates");
        }

        long rentalDays =
                Math.max(
                        1,
                        Duration.between(
                                request.pickupDateTime(),
                                request.returnDateTime()
                        ).toDays()
                );

        BigDecimal estimatedRentalPrice =
                vehicle.getDailyPrice()
                        .multiply(BigDecimal.valueOf(rentalDays));

        BigDecimal depositAmount =
                estimatedRentalPrice.multiply(BigDecimal.valueOf(0.30))
                        .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalEstimatedPrice =
                estimatedRentalPrice.add(depositAmount);

        LocalDateTime now = LocalDateTime.now();

        Reservation reservation = Reservation.builder()
                .tenant(vehicle.getTenant())
                .vehicle(vehicle)

                .customerFullName(request.customerFullName())
                .customerPhone(request.customerPhone())
                .customerEmail(request.customerEmail())
                .customerIdentityNumber(request.customerIdentityNumber())

                .pickupDateTime(request.pickupDateTime())
                .returnDateTime(request.returnDateTime())

                .dailyPriceSnapshot(vehicle.getDailyPrice())
                .dailyKmLimitSnapshot(vehicle.getDailyKmLimit())
                .extraKmPricePerKmSnapshot(vehicle.getExtraKmPricePerKm())

                .depositAmount(depositAmount)
                .estimatedRentalPrice(estimatedRentalPrice)
                .totalEstimatedPrice(totalEstimatedPrice)

                .status(ReservationStatus.PENDING_PAYMENT)
                .active(true)

                .createdAt(now)
                .updatedAt(now)
                .build();

        Reservation savedReservation =
                reservationRepository.save(reservation);

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                vehicle.getTenant() != null ? vehicle.getTenant().getId() : null,
                AuditAction.RESERVATION_CREATED,
                "Reservation",
                savedReservation.getId(),
                "Reservation created for vehicle: " + vehicle.getPlateNumber()
        ));

        return mapToResponse(savedReservation);
    }
    public List<ReservationResponse> getAllReservations() {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isSuperAdmin(currentUser)) {
            return reservationRepository.findByActiveTrue()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        Long tenantId = currentUserService.getCurrentTenantId();

        return reservationRepository.findByTenant_IdAndActiveTrue(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = findReservationByIdWithTenantIsolation(id);
        return mapToResponse(reservation);
    }
    public void cancelReservation(Long id) {
        User currentUser = currentUserService.getCurrentUser();

        Reservation reservation = findReservationByIdWithTenantIsolation(id);

        if (reservation.getStatus() == ReservationStatus.CONVERTED_TO_RENTAL) {
            throw new BusinessException("Converted reservation cannot be cancelled");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException("Reservation is already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setActive(false);
        reservation.setUpdatedAt(LocalDateTime.now());

        Reservation cancelledReservation = reservationRepository.save(reservation);

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                cancelledReservation.getTenant() != null ? cancelledReservation.getTenant().getId() : null,
                AuditAction.RESERVATION_CANCELLED,
                "Reservation",
                cancelledReservation.getId(),
                "Reservation cancelled for vehicle: " +
                        (cancelledReservation.getVehicle() != null
                                ? cancelledReservation.getVehicle().getPlateNumber()
                                : "unknown")
        ));
    }
    private Reservation findReservationByIdWithTenantIsolation(Long reservationId) {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isSuperAdmin(currentUser)) {
            return reservationRepository.findByIdAndActiveTrue(reservationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        }

        Long tenantId = currentUserService.getCurrentTenantId();

        return reservationRepository.findByIdAndTenant_IdAndActiveTrue(reservationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }
    private Vehicle findVehicleByIdWithTenantIsolation(Long vehicleId) {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isSuperAdmin(currentUser)) {
            return vehicleRepository.findByIdAndActiveTrue(vehicleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        }

        Long tenantId = currentUserService.getCurrentTenantId();

        return vehicleRepository.findByIdAndTenant_IdAndActiveTrue(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    }

    private Long getCurrentTenantId() {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getTenant() == null) {
            throw new BusinessException("Current user is not assigned to any tenant");
        }

        return currentUser.getTenant().getId();
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),

                reservation.getTenant() != null ? reservation.getTenant().getId() : null,
                reservation.getTenant() != null ? reservation.getTenant().getCompanyName() : null,

                reservation.getVehicle() != null ? reservation.getVehicle().getId() : null,
                reservation.getVehicle() != null ? reservation.getVehicle().getBrand() : null,
                reservation.getVehicle() != null ? reservation.getVehicle().getModel() : null,
                reservation.getVehicle() != null ? reservation.getVehicle().getPlateNumber() : null,

                reservation.getCustomerFullName(),
                reservation.getCustomerPhone(),
                reservation.getCustomerEmail(),
                reservation.getCustomerIdentityNumber(),

                reservation.getPickupDateTime(),
                reservation.getReturnDateTime(),

                reservation.getDailyPriceSnapshot(),
                reservation.getDailyKmLimitSnapshot(),
                reservation.getExtraKmPricePerKmSnapshot(),

                reservation.getDepositAmount(),
                reservation.getEstimatedRentalPrice(),
                reservation.getTotalEstimatedPrice(),

                reservation.getStatus().name(),
                reservation.getActive(),

                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
