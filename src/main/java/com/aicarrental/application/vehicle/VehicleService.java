package com.aicarrental.application.vehicle;

import com.aicarrental.api.vehicle.request.CreateVehicleRequest;
import com.aicarrental.api.vehicle.request.UpdateVehicleRequest;
import com.aicarrental.api.vehicle.response.VehicleResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.auth.Role;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final AuditEventPublisher auditEventPublisher;

    private User getCurrentUser(){
Object principal = SecurityContextHolder
        .getContext()
        .getAuthentication()
        .getPrincipal();

if(!(principal instanceof User user)){
    throw new BusinessException("Authenticated user could not be found");
}
return user;
    }

    private Boolean isSuperAdmin(User user) {

        return user.getRole()== Role.SUPER_ADMIN;

    }


    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        User currentUser = getCurrentUser();

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
                .status(request.status())
                .active(true)
                .tenant(currentTenant)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        System.out.println("VEHICLE AUDIT PUBLISHING: " + savedVehicle.getId());
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

        return mapToResponse(savedVehicle);
    }
    public List<VehicleResponse> getAllVehicles() {
        User currentUser = getCurrentUser();

        if (isSuperAdmin(currentUser)) {
            return vehicleRepository.findByActiveTrue()
                    .stream()
                    .map(this::mapToResponse)
                    .toList();
        }

        Long tenantId = getCurrentTenantId();

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
        User currentUser = getCurrentUser();

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

        return mapToResponse(updatedVehicle);
    }
    public void deleteVehicle(Long id) {
        User currentUser = getCurrentUser();

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
                vehicle.getStatus().name(),
                vehicle.getActive(),
                vehicle.getTenant() != null ? vehicle.getTenant().getId() : null,
                vehicle.getTenant() != null ? vehicle.getTenant().getCompanyName() : null,
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }
    private Long getCurrentTenantId() {
        User currentUser = getCurrentUser();

        if (currentUser.getTenant() == null) {
            throw new BusinessException("Current user is not assigned to any tenant");
        }

        return currentUser.getTenant().getId();
    }
    private Vehicle findVehicleByIdWithTenantIsolation(Long vehicleId) {
        User currentUser = getCurrentUser();

        if (isSuperAdmin(currentUser)) {
            return vehicleRepository.findByIdAndActiveTrue(vehicleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        }

        Long tenantId = getCurrentTenantId();

        return vehicleRepository.findByIdAndTenant_IdAndActiveTrue(vehicleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    }

}
