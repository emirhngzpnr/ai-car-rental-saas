package com.aicarrental.api.rental.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record StartRentalRequest(@NotNull(message = "Reservation id is required")
                                 Long reservationId,

                                 @NotNull(message = "Actual pickup date is required")
                                 LocalDateTime actualPickupDateTime,

                                 @NotNull(message = "Start mileage is required")
                                 @Min(value = 0, message = "Start mileage cannot be negative")
                                 Integer startMileage) {
}
