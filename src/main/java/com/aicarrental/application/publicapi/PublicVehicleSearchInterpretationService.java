package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.response.PublicVehicleSearchCriteriaResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchInterpretationResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchInterpretResponse;
import com.aicarrental.application.publicapi.ai.PriceIntent;
import com.aicarrental.application.publicapi.ai.SegmentIntent;
import com.aicarrental.application.publicapi.ai.VehicleSearchAiClient;
import com.aicarrental.application.publicapi.ai.VehicleSearchAiResult;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ServiceUnavailableException;
import com.aicarrental.domain.vehicle.FuelType;
import com.aicarrental.domain.vehicle.TransmissionType;
import com.aicarrental.domain.vehicle.VehicleCategory;
import com.aicarrental.infrastructure.persistence.VehiclePriceDistributionProjection;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicVehicleSearchInterpretationService {
    private static final int MAX_QUERY_LENGTH = 500;
    private static final int MAX_TEXT_LENGTH = 80;
    private static final int MAX_SUMMARY_LENGTH = 240;
    private static final int MAX_WARNING_LENGTH = 160;
    private static final int MAX_WARNINGS = 6;
    private static final long MIN_PRICE_SAMPLE_SIZE = 3;
    private static final Set<String> ALLOWED_SORTS =
            Set.of("recommended", "priceAsc", "priceDesc", "kmLimitDesc");

    private final VehicleSearchAiClient aiClient;
    private final VehicleRepository vehicleRepository;

    public PublicVehicleSearchInterpretResponse interpret(
            String query,
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime,
            String requestLocation
    ) {
        String normalizedQuery = normalizeQuery(query);
        PublicVehicleService.validateDateRange(pickupDateTime, returnDateTime);

        VehicleSearchAiResult result;
        try {
            result = aiClient.interpret(normalizedQuery);
        } catch (Exception exception) {
            log.warn("AI marketplace interpretation failed: {}", exception.getClass().getSimpleName());
            throw unavailable();
        }
        if (result == null) {
            throw unavailable();
        }

        validateNumbers(result);
        List<String> warnings = sanitizeWarnings(result.warnings());
        List<String> inferences = new ArrayList<>();

        VehicleCategory explicitCategory = parseEnum(
                result.category(), VehicleCategory.class, "category", warnings
        );
        TransmissionType transmission = parseEnum(
                result.transmission(), TransmissionType.class, "transmission", warnings
        );
        FuelType fuelType = parseEnum(
                result.fuelType(), FuelType.class, "fuel type", warnings
        );
        PriceIntent priceIntent = parseEnum(
                result.priceIntent(), PriceIntent.class, "price intent", warnings
        );
        SegmentIntent segmentIntent = parseEnum(
                result.segmentIntent(), SegmentIntent.class, "segment intent", warnings
        );

        List<VehicleCategory> categories = resolveCategories(
                explicitCategory,
                segmentIntent,
                inferences
        );
        String location = firstNonBlank(normalizeText(result.location()), normalizeText(requestLocation));
        String brand = normalizeText(result.brand());
        String model = normalizeText(result.model());
        String sort = normalizeSort(result.sort(), warnings);
        BigDecimal minPrice = result.minDailyPrice();
        BigDecimal maxPrice = result.maxDailyPrice();

        if (priceIntent != null && minPrice == null && maxPrice == null) {
            PriceRange priceRange = resolveRelativePrice(
                    priceIntent,
                    pickupDateTime,
                    returnDateTime,
                    result.minDailyKmLimit(),
                    brand,
                    model,
                    categories,
                    transmission,
                    fuelType,
                    result.minSeats(),
                    location,
                    warnings
            );
            if (minPrice == null) {
                minPrice = priceRange.minPrice();
            }
            if (maxPrice == null) {
                maxPrice = priceRange.maxPrice();
            }
            if (priceRange.resolved()) {
                inferences.add(priceInference(priceIntent));
            }
        }

        var criteria = new PublicVehicleSearchCriteriaResponse(
                minPrice,
                maxPrice,
                result.minDailyKmLimit(),
                brand,
                model,
                categories,
                transmission,
                fuelType,
                result.minSeats(),
                location,
                sort
        );
        var interpretation = new PublicVehicleSearchInterpretationResponse(priceIntent, segmentIntent);
        String summary = result.summary() == null || result.summary().isBlank()
                ? "Filters were extracted from your request."
                : truncate(result.summary().trim(), MAX_SUMMARY_LENGTH);

        return new PublicVehicleSearchInterpretResponse(
                criteria,
                interpretation,
                summary,
                List.copyOf(inferences),
                List.copyOf(warnings)
        );
    }

    private List<VehicleCategory> resolveCategories(
            VehicleCategory explicitCategory,
            SegmentIntent segmentIntent,
            List<String> inferences
    ) {
        if (explicitCategory != null) {
            return List.of(explicitCategory);
        }
        if (segmentIntent == null) {
            return List.of();
        }

        List<VehicleCategory> categories = switch (segmentIntent) {
            case CITY -> List.of(VehicleCategory.ECONOMY, VehicleCategory.COMPACT);
            case MID_RANGE -> List.of(VehicleCategory.COMPACT, VehicleCategory.SEDAN);
            case FAMILY -> List.of(VehicleCategory.SEDAN, VehicleCategory.SUV, VehicleCategory.VAN);
            case SPACIOUS -> List.of(VehicleCategory.SUV, VehicleCategory.VAN);
            case PREMIUM -> List.of(VehicleCategory.LUXURY);
        };
        inferences.add(segmentInference(segmentIntent, categories));
        return categories;
    }

    private PriceRange resolveRelativePrice(
            PriceIntent intent,
            LocalDateTime pickupDateTime,
            LocalDateTime returnDateTime,
            Integer minDailyKmLimit,
            String brand,
            String model,
            List<VehicleCategory> categories,
            TransmissionType transmission,
            FuelType fuelType,
            Integer minSeats,
            String location,
            List<String> warnings
    ) {
        boolean categoriesEmpty = categories.isEmpty();
        List<String> queryCategories = categoriesEmpty
                ? Arrays.stream(VehicleCategory.values()).map(Enum::name).toList()
                : categories.stream().map(Enum::name).toList();

        VehiclePriceDistributionProjection distribution =
                vehicleRepository.calculateAvailablePriceDistribution(
                        pickupDateTime,
                        returnDateTime,
                        minDailyKmLimit,
                        normalizeForQuery(brand),
                        normalizeForQuery(model),
                        queryCategories,
                        categoriesEmpty,
                        transmission == null ? "" : transmission.name(),
                        fuelType == null ? "" : fuelType.name(),
                        minSeats,
                        normalizeForQuery(location)
                );

        if (distribution == null || distribution.getSampleCount() < MIN_PRICE_SAMPLE_SIZE) {
            addWarning(warnings, "Relative price could not be applied because too few matching vehicles are available.");
            return PriceRange.unresolved();
        }

        return switch (intent) {
            case BUDGET -> new PriceRange(null, money(distribution.getP30()), true);
            case AFFORDABLE -> new PriceRange(null, money(distribution.getP45()), true);
            case NOT_EXPENSIVE -> new PriceRange(null, money(distribution.getP60()), true);
            case MID_RANGE -> new PriceRange(money(distribution.getP30()), money(distribution.getP70()), true);
            case PREMIUM -> new PriceRange(money(distribution.getP75()), null, true);
        };
    }

    private String segmentInference(SegmentIntent intent, List<VehicleCategory> categories) {
        String values = categories.stream()
                .map(category -> title(category.name()))
                .reduce((left, right) -> left + " or " + right)
                .orElse("");
        return title(intent.name()) + " was interpreted as " + values + ".";
    }

    private String priceInference(PriceIntent intent) {
        return title(intent.name())
                + " was calculated from current prices of matching available vehicles.";
    }

    private String title(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private BigDecimal money(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new BusinessException("Search query is required");
        }
        String normalized = query.trim();
        if (normalized.length() > MAX_QUERY_LENGTH) {
            throw new BusinessException("Search query cannot exceed 500 characters");
        }
        return normalized;
    }

    private void validateNumbers(VehicleSearchAiResult result) {
        if (isNegative(result.minDailyPrice()) || isNegative(result.maxDailyPrice())
                || result.minDailyKmLimit() != null && result.minDailyKmLimit() < 0
                || result.minSeats() != null && result.minSeats() < 1) {
            throw unavailable();
        }
        if (result.minDailyPrice() != null && result.maxDailyPrice() != null
                && result.minDailyPrice().compareTo(result.maxDailyPrice()) > 0) {
            throw unavailable();
        }
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw unavailable();
        }
        return normalized;
    }

    private String firstNonBlank(String primary, String fallback) {
        return primary != null ? primary : fallback;
    }

    private String normalizeForQuery(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private <E extends Enum<E>> E parseEnum(
            String value,
            Class<E> enumType,
            String fieldName,
            List<String> warnings
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            addWarning(warnings, "Ignored unsupported " + fieldName + " filter.");
            return null;
        }
    }

    private String normalizeSort(String value, List<String> warnings) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (!ALLOWED_SORTS.contains(normalized)) {
            addWarning(warnings, "Ignored unsupported sort option.");
            return null;
        }
        return normalized;
    }

    private List<String> sanitizeWarnings(List<String> source) {
        List<String> warnings = new ArrayList<>();
        if (source == null) {
            return warnings;
        }
        source.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .map(item -> truncate(item, MAX_WARNING_LENGTH))
                .limit(MAX_WARNINGS)
                .forEach(warnings::add);
        return warnings;
    }

    private void addWarning(List<String> warnings, String warning) {
        if (warnings.size() < MAX_WARNINGS) {
            warnings.add(warning);
        }
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private ServiceUnavailableException unavailable() {
        return new ServiceUnavailableException(
                "AI vehicle search is temporarily unavailable. Manual filters are still available."
        );
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice, boolean resolved) {
        private static PriceRange unresolved() {
            return new PriceRange(null, null, false);
        }
    }
}
