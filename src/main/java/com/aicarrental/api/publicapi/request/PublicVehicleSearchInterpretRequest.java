package com.aicarrental.api.publicapi.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PublicVehicleSearchInterpretRequest(
        @NotBlank(message = "Search query is required")
        @Size(max = 500, message = "Search query cannot exceed 500 characters")
        String query,
        LocalDateTime pickupDateTime,
        LocalDateTime returnDateTime,
        @Size(max = 80, message = "Location cannot exceed 80 characters")
        String location
) {
}
