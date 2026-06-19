package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.response.PublicVehicleSearchCriteriaResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchInterpretResponse;
import com.aicarrental.application.publicapi.ai.VehicleSearchAiClient;
import com.aicarrental.application.publicapi.ai.VehicleSearchAiResult;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ServiceUnavailableException;
import com.aicarrental.domain.vehicle.FuelType;
import com.aicarrental.domain.vehicle.TransmissionType;
import com.aicarrental.domain.vehicle.VehicleCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class PublicVehicleSearchInterpretationService {
    private static final int MAX_QUERY_LENGTH = 500;
    private static final int MAX_TEXT_LENGTH = 80;
    private static final int MAX_SUMMARY_LENGTH = 240;
    private static final int MAX_WARNING_LENGTH = 160;
    private static final int MAX_WARNINGS = 6;
    private static final Set<String> ALLOWED_SORTS =
            Set.of("recommended", "priceAsc", "priceDesc", "kmLimitDesc");

    private final VehicleSearchAiClient aiClient;

    public PublicVehicleSearchInterpretResponse interpret(String query) {
        String normalizedQuery = normalizeQuery(query);
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

        VehicleCategory category = parseEnum(
                result.category(), VehicleCategory.class, "category", warnings
        );
        TransmissionType transmission = parseEnum(
                result.transmission(), TransmissionType.class, "transmission", warnings
        );
        FuelType fuelType = parseEnum(
                result.fuelType(), FuelType.class, "fuel type", warnings
        );
        String sort = normalizeSort(result.sort(), warnings);

        var criteria = new PublicVehicleSearchCriteriaResponse(
                result.minDailyPrice(),
                result.maxDailyPrice(),
                result.minDailyKmLimit(),
                normalizeText(result.brand()),
                normalizeText(result.model()),
                category,
                transmission,
                fuelType,
                result.minSeats(),
                normalizeText(result.location()),
                sort
        );

        String summary = result.summary() == null || result.summary().isBlank()
                ? "Filters were extracted from your request."
                : truncate(result.summary().trim(), MAX_SUMMARY_LENGTH);

        return new PublicVehicleSearchInterpretResponse(criteria, summary, List.copyOf(warnings));
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
}
