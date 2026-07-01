package com.aicarrental.api.publicapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PublicAssistantQueryRequest(
        @NotBlank @Size(max = 500) String query,
        @Size(max = 120) String tenantSlug,
        LocalDateTime pickupDateTime,
        LocalDateTime returnDateTime,
        BigDecimal minDailyPrice,
        BigDecimal maxDailyPrice,
        Integer minDailyKmLimit,
        String brand,
        String model,
        List<String> categories,
        String transmission,
        String fuelType,
        Integer minSeats,
        String location,
        String sort
) {
}
