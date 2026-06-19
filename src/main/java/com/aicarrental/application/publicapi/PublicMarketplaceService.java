package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.response.PublicInsurancePackageResponse;
import com.aicarrental.api.publicapi.response.PublicMarketplaceSearchResponse;
import com.aicarrental.api.publicapi.response.PublicMarketplaceVehicleDetailResponse;
import com.aicarrental.api.publicapi.response.PublicMarketplaceVehicleResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.vehicle.FuelType;
import com.aicarrental.domain.vehicle.TransmissionType;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.domain.vehicle.VehicleCategory;
import com.aicarrental.infrastructure.persistence.InsurancePackageRepository;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicMarketplaceService {
    private final VehicleRepository vehicleRepository;
    private final InsurancePackageRepository insurancePackageRepository;

    public PublicMarketplaceSearchResponse search(
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime,
            BigDecimal minDailyPrice,
            BigDecimal maxDailyPrice,
            Integer minDailyKmLimit,
            String brand,
            String model,
            VehicleCategory category,
            TransmissionType transmission,
            FuelType fuelType,
            Integer minSeats,
            String location,
            String sort,
            int page,
            int size
    ) {
        PublicVehicleService.validateDateRange(pickupDateTime, returnDateTime);
        validateFilters(minDailyPrice, maxDailyPrice, minDailyKmLimit, minSeats);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        PageRequest pageable = PageRequest.of(safePage, safeSize, resolveSort(sort));

        Page<Vehicle> result = vehicleRepository.searchPublicMarketplace(
                pickupDateTime,
                returnDateTime,
                minDailyPrice,
                maxDailyPrice,
                minDailyKmLimit,
                normalize(brand),
                normalize(model),
                category,
                transmission,
                fuelType,
                minSeats,
                normalize(location),
                pageable
        );

        return new PublicMarketplaceSearchResponse(
                result.getContent().stream().map(this::mapVehicle).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public PublicMarketplaceVehicleDetailResponse getVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdAndActiveTrueAndTenant_ActiveTrue(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (vehicle.getStatus() != com.aicarrental.domain.vehicle.VehicleStatus.AVAILABLE) {
            throw new ResourceNotFoundException("Vehicle not found");
        }

        var packages = insurancePackageRepository.findByTenant_IdAndActiveTrue(vehicle.getTenant().getId())
                .stream()
                .map(item -> new PublicInsurancePackageResponse(
                        item.getId(),
                        item.getType().name(),
                        item.getName(),
                        item.getCoverageDescription(),
                        item.getDailyPrice()
                ))
                .toList();

        return new PublicMarketplaceVehicleDetailResponse(
                vehicle.getId(),
                vehicle.getTenant().getSlug(),
                vehicle.getTenant().getCompanyName(),
                vehicle.getTenant().getEmail(),
                vehicle.getTenant().getPhoneNumber(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getProductionYear(),
                vehicle.getDailyPrice(),
                vehicle.getDailyKmLimit(),
                vehicle.getExtraKmPricePerKm(),
                name(vehicle.getCategory()),
                name(vehicle.getTransmission()),
                name(vehicle.getFuelType()),
                vehicle.getSeatCount(),
                vehicle.getLocation(),
                vehicle.getImageUrl(),
                packages
        );
    }

    private PublicMarketplaceVehicleResponse mapVehicle(Vehicle vehicle) {
        return new PublicMarketplaceVehicleResponse(
                vehicle.getId(),
                vehicle.getTenant().getSlug(),
                vehicle.getTenant().getCompanyName(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getProductionYear(),
                vehicle.getDailyPrice(),
                vehicle.getDailyKmLimit(),
                vehicle.getExtraKmPricePerKm(),
                name(vehicle.getCategory()),
                name(vehicle.getTransmission()),
                name(vehicle.getFuelType()),
                vehicle.getSeatCount(),
                vehicle.getLocation(),
                vehicle.getImageUrl()
        );
    }

    private void validateFilters(BigDecimal minPrice, BigDecimal maxPrice, Integer minKm, Integer minSeats) {
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw new BusinessException("Price filters cannot be negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException("Minimum price cannot exceed maximum price");
        }
        if (minKm != null && minKm < 0 || minSeats != null && minSeats < 1) {
            throw new BusinessException("Kilometer and seat filters are invalid");
        }
    }

    private Sort resolveSort(String value) {
        return switch (value == null ? "recommended" : value) {
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "dailyPrice");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "dailyPrice");
            case "kmLimitDesc" -> Sort.by(Sort.Direction.DESC, "dailyKmLimit");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String name(Enum<?> value) {
        return value != null ? value.name() : null;
    }
}
