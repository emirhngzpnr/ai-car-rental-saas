package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.response.PublicAvailableVehicleResponse;
import com.aicarrental.api.publicapi.response.PublicInsurancePackageResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleDetailResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.insurance.InsurancePackage;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.domain.vehicle.VehicleStatus;
import com.aicarrental.infrastructure.persistence.InsurancePackageRepository;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicVehicleService {
    private final PublicTenantService publicTenantService;
    private final VehicleRepository vehicleRepository;
    private final InsurancePackageRepository insurancePackageRepository;

    public List<PublicAvailableVehicleResponse> getAvailableVehicles(
            String tenantSlug,
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime
    ) {
        validateDateRange(pickupDateTime, returnDateTime);
        Tenant tenant = publicTenantService.findActiveTenant(tenantSlug);

        return vehicleRepository.findAvailableVehicles(tenant.getId(), pickupDateTime, returnDateTime)
                .stream()
                .map(this::mapAvailableVehicle)
                .toList();
    }

    public PublicVehicleDetailResponse getVehicleDetail(String tenantSlug, Long vehicleId) {
        Tenant tenant = publicTenantService.findActiveTenant(tenantSlug);
        Vehicle vehicle = vehicleRepository.findByIdAndTenant_IdAndActiveTrue(vehicleId, tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ResourceNotFoundException("Vehicle not found");
        }

        List<PublicInsurancePackageResponse> insurancePackages =
                insurancePackageRepository.findByTenant_IdAndActiveTrue(tenant.getId())
                        .stream()
                        .map(this::mapInsurancePackage)
                        .toList();

        return new PublicVehicleDetailResponse(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getProductionYear(),
                maskPlateNumber(vehicle.getPlateNumber()),
                vehicle.getDailyPrice(),
                vehicle.getDailyKmLimit(),
                vehicle.getExtraKmPricePerKm(),
                insurancePackages
        );
    }

    static void validateDateRange(LocalDateTime pickupDateTime, LocalDateTime returnDateTime) {
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

    static String maskPlateNumber(String plateNumber) {
        if (plateNumber == null || plateNumber.length() <= 4) {
            return "****";
        }

        return plateNumber.substring(0, 2) + "***" + plateNumber.substring(plateNumber.length() - 2);
    }

    private PublicAvailableVehicleResponse mapAvailableVehicle(Vehicle vehicle) {
        return new PublicAvailableVehicleResponse(
                vehicle.getId(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getProductionYear(),
                maskPlateNumber(vehicle.getPlateNumber()),
                vehicle.getDailyPrice(),
                vehicle.getDailyKmLimit(),
                vehicle.getExtraKmPricePerKm()
        );
    }

    private PublicInsurancePackageResponse mapInsurancePackage(InsurancePackage insurancePackage) {
        return new PublicInsurancePackageResponse(
                insurancePackage.getId(),
                insurancePackage.getType().name(),
                insurancePackage.getName(),
                insurancePackage.getCoverageDescription(),
                insurancePackage.getDailyPrice()
        );
    }
}
