package com.aicarrental.application.vehicle;

import com.aicarrental.api.vehicle.response.AvailableVehicleResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleAvailabilityService {
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;

    public List<AvailableVehicleResponse> searchAvailableVehicles(
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime
    ) {
        validateDateRange(pickupDateTime, returnDateTime);

        Long tenantId = currentUserService.getCurrentTenantId();

        return vehicleRepository
                .findAvailableVehicles(
                        tenantId,
                        pickupDateTime,
                        returnDateTime
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void validateDateRange(
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime
    ) {
        if (pickupDateTime == null || returnDateTime == null) {
            throw new BusinessException("Pickup and return date time are required");
        }

        if (!returnDateTime.isAfter(pickupDateTime)) {
            throw new BusinessException("Return date time must be after pickup date time");
        }

        if (pickupDateTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Pickup date time cannot be in the past");
        }
    }

    private AvailableVehicleResponse mapToResponse(Vehicle vehicle) {
        return new AvailableVehicleResponse(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getDailyPrice(),
                vehicle.getCurrentMileage(),
                vehicle.getStatus().name()
        );
    }
}
