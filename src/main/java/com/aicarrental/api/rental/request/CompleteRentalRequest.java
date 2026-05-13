package com.aicarrental.api.rental.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CompleteRentalRequest(
        @NotNull(message = "Actual return date is required")
        LocalDateTime actualReturnDateTime,

        @NotNull(message = "End mileage is required")
        @Min(value = 0, message = "End mileage cannot be negative")
        Integer endMileage) {
}
