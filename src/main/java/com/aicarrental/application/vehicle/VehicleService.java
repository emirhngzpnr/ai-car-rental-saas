package com.aicarrental.application.vehicle;

import com.aicarrental.api.vehicle.request.CreateVehicleRequest;
import com.aicarrental.api.vehicle.request.UpdateVehicleRequest;
import com.aicarrental.api.vehicle.response.VehicleResponse;
import com.aicarrental.application.report.ReportCacheInvalidator;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.domain.vehicle.VehicleStatus;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final CurrentUserService currentUserService;
    private final ReportCacheInvalidator reportCacheInvalidator;


    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        validateImageUrl(request.imageUrl());

        Tenant currentTenant = currentUser.getTenant();

        if (currentTenant == null) {
            throw new BusinessException("Current user is not assigned to any tenant");
        }

        if (vehicleRepository.existsByPlateNumber(request.plateNumber())) {
            throw new BusinessException("Plate number already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        Vehicle vehicle = Vehicle.builder()
                .brand(request.brand())
                .model(request.model())
                .plateNumber(request.plateNumber())
                .productionYear(request.productionYear())
                .currentMileage(request.currentMileage())
                .dailyPrice(request.dailyPrice())
                .dailyKmLimit(request.dailyKmLimit())
                .extraKmPricePerKm(request.extraKmPricePerKm())
                .category(request.category())
                .transmission(request.transmission())
                .fuelType(request.fuelType())
                .seatCount(request.seatCount())
                .location(request.location())
                .imageUrl(normalizeOptional(request.imageUrl()))
                .status(request.status())
                .active(true)
                .tenant(currentTenant)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                currentTenant.getId(),
                AuditAction.VEHICLE_CREATED,
                "Vehicle",
                savedVehicle.getId(),
                "Vehicle created: " + savedVehicle.getPlateNumber()
        ));
        reportCacheInvalidator.evictAfterCommit();

        return mapToResponse(savedVehicle);
    }
    public List<VehicleResponse> getAllVehicles() {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isSuperAdmin(currentUser)) {
            return vehicleRepository.findByActiveTrue()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        Long tenantId = currentUserService.getCurrentTenantId();
        return vehicleRepository.findByTenant_IdAndActiveTrue(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = findVehicleByIdWithTenantIsolation(id);
        return mapToResponse(vehicle);
    }
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        validateImageUrl(request.imageUrl());

        Vehicle vehicle = findVehicleByIdWithTenantIsolation(id);

        if (vehicleRepository.existsByPlateNumberAndIdNot(request.plateNumber(), id)) {
            throw new BusinessException("Plate number already exists");
        }

        vehicle.setBrand(request.brand());
        vehicle.setModel(request.model());
        vehicle.setPlateNumber(request.plateNumber());
        vehicle.setProductionYear(request.productionYear());
        vehicle.setCurrentMileage(request.currentMileage());
        vehicle.setDailyPrice(request.dailyPrice());
        vehicle.setDailyKmLimit(request.dailyKmLimit());
        vehicle.setExtraKmPricePerKm(request.extraKmPricePerKm());
        vehicle.setCategory(request.category());
        vehicle.setTransmission(request.transmission());
        vehicle.setFuelType(request.fuelType());
        vehicle.setSeatCount(request.seatCount());
        vehicle.setLocation(request.location());
        vehicle.setImageUrl(normalizeOptional(request.imageUrl()));
        vehicle.setStatus(request.status());
        vehicle.setActive(request.active());
        vehicle.setUpdatedAt(LocalDateTime.now());

        Vehicle updatedVehicle = vehicleRepository.save(vehicle);

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                updatedVehicle.getTenant() != null ? updatedVehicle.getTenant().getId() : null,
                AuditAction.VEHICLE_UPDATED,
                "Vehicle",
                updatedVehicle.getId(),
                "Vehicle updated: " + updatedVehicle.getPlateNumber()
        ));
        reportCacheInvalidator.evictAfterCommit();

        return mapToResponse(updatedVehicle);
    }
    public void deleteVehicle(Long id) {
        User currentUser = currentUserService.getCurrentUser();

        Vehicle vehicle = findVehicleByIdWithTenantIsolation(id);

        vehicle.setActive(false);
        vehicle.setUpdatedAt(LocalDateTime.now());

        Vehicle deletedVehicle = vehicleRepository.save(vehicle);

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                deletedVehicle.getTenant() != null ? deletedVehicle.getTenant().getId() : null,
                AuditAction.VEHICLE_DELETED,
                "Vehicle",
                deletedVehicle.getId(),
                "Vehicle soft deleted: " + deletedVehicle.getPlateNumber()
        ));
        reportCacheInvalidator.evictAfterCommit();
    }
    public List<VehicleResponse> getAvailableVehicles(
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime
    ) {
        LocalDateTime now = LocalDateTime.now();
        if (pickupDateTime.isBefore(now)) {
            throw new BusinessException("Pickup date cannot be in the past");
        }
        if (returnDateTime.isBefore(pickupDateTime) || returnDateTime.isEqual(pickupDateTime)) {
            throw new BusinessException("Return date must be after pickup date");
        }

        User currentUser = currentUserService.getCurrentUser();

        List<ReservationStatus> blockingStatuses = List.of(
                ReservationStatus.PENDING_PAYMENT,
                ReservationStatus.CONFIRMED
        );

        List<Vehicle> availableVehicles;

        if (currentUserService.isSuperAdmin(currentUser)) {
            availableVehicles = vehicleRepository.findAvailableVehicles(
                    pickupDateTime,
                    returnDateTime,
                    blockingStatuses,
                    VehicleStatus.AVAILABLE
            );
        } else {
            Long tenantId = currentUserService.getCurrentTenantId();

            availableVehicles = vehicleRepository.findAvailableVehiclesByTenant(
                    tenantId,
                    pickupDateTime,
                    returnDateTime,
                    blockingStatuses,
                    VehicleStatus.AVAILABLE
            );
        }

        return availableVehicles
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getPlateNumber(),
                vehicle.getProductionYear(),
                vehicle.getCurrentMileage(),
                vehicle.getDailyPrice(),
                vehicle.getDailyKmLimit(),
                vehicle.getExtraKmPricePerKm(),
                vehicle.getCategory() != null ? vehicle.getCategory().name() : null,
                vehicle.getTransmission() != null ? vehicle.getTransmission().name() : null,
                vehicle.getFuelType() != null ? vehicle.getFuelType().name() : null,
                vehicle.getSeatCount(),
                vehicle.getLocation(),
                vehicle.getImageUrl(),
                vehicle.getStatus().name(),
                vehicle.getActive(),
                vehicle.getTenant() != null ? vehicle.getTenant().getId() : null,
                vehicle.getTenant() != null ? vehicle.getTenant().getCompanyName() : null,
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
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

    private void validateImageUrl(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()
                && !imageUrl.matches("^https?://.+")) {
            throw new BusinessException("Image URL must use HTTP or HTTPS");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
