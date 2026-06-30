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
import com.aicarrental.infrastructure.persistence.VehicleReviewRepository;
import com.aicarrental.infrastructure.persistence.projection.VehicleReviewSummaryProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicMarketplaceService {
    private final VehicleRepository vehicleRepository;
    private final InsurancePackageRepository insurancePackageRepository;
    private final VehicleReviewRepository reviewRepository;

    public PublicMarketplaceSearchResponse search(
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime,
            BigDecimal minDailyPrice,
            BigDecimal maxDailyPrice,
            Integer minDailyKmLimit,
            String brand,
            String model,
            VehicleCategory category,
            List<VehicleCategory> categories,
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
        List<VehicleCategory> resolvedCategories = resolveCategories(category, categories);
        boolean categoriesEmpty = resolvedCategories.isEmpty();
        List<VehicleCategory> queryCategories = categoriesEmpty
                ? Arrays.asList(VehicleCategory.values())
                : resolvedCategories;

        Page<Vehicle> result = vehicleRepository.searchPublicMarketplace(
                pickupDateTime,
                returnDateTime,
                minDailyPrice,
                maxDailyPrice,
                minDailyKmLimit,
                normalize(brand),
                normalize(model),
                queryCategories,
                categoriesEmpty,
                transmission,
                fuelType,
                minSeats,
                normalize(location),
                pageable
        );

        Map<Long, VehicleReviewSummaryProjection> summaries = reviewSummaries(
                result.getContent().stream().map(Vehicle::getId).toList()
        );

        return new PublicMarketplaceSearchResponse(
                result.getContent().stream().map(vehicle -> mapVehicle(vehicle, summaries)).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private List<VehicleCategory> resolveCategories(
            VehicleCategory category,
            List<VehicleCategory> categories
    ) {
        List<VehicleCategory> resolved = new ArrayList<>();
        if (categories != null) {
            categories.stream().filter(item -> item != null && !resolved.contains(item)).forEach(resolved::add);
        }
        if (category != null && !resolved.contains(category)) {
            resolved.add(category);
        }
        return List.copyOf(resolved);
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

        VehicleReviewSummaryProjection summary = reviewSummaries(List.of(vehicle.getId())).get(vehicle.getId());

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
                averageRating(summary),
                reviewCount(summary),
                packages
        );
    }

    private PublicMarketplaceVehicleResponse mapVehicle(
            Vehicle vehicle,
            Map<Long, VehicleReviewSummaryProjection> summaries
    ) {
        VehicleReviewSummaryProjection summary = summaries.get(vehicle.getId());
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
                vehicle.getImageUrl(),
                averageRating(summary),
                reviewCount(summary)
        );
    }

    private Map<Long, VehicleReviewSummaryProjection> reviewSummaries(List<Long> vehicleIds) {
        if (vehicleIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.summarizeByVehicleIds(vehicleIds)
                .stream()
                .collect(Collectors.toMap(VehicleReviewSummaryProjection::getVehicleId, Function.identity()));
    }

    private BigDecimal averageRating(VehicleReviewSummaryProjection summary) {
        if (summary == null || summary.getAverageRating() == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(summary.getAverageRating()).setScale(1, RoundingMode.HALF_UP);
    }

    private Long reviewCount(VehicleReviewSummaryProjection summary) {
        return summary == null || summary.getReviewCount() == null ? 0L : summary.getReviewCount();
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
