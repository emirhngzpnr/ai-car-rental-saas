package com.aicarrental.api.reservation.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateReservationRequest(@NotNull(message = "Vehicle id is required")
                                       Long vehicleId,

                                       @NotBlank(message = "Customer full name is required")
                                       String customerFullName,

                                       @NotBlank(message = "Customer phone is required")
                                       String customerPhone,

                                       @Email(message = "Customer email format is invalid")
                                       @NotBlank(message = "Customer email is required")
                                       String customerEmail,

                                       @NotBlank(message = "Customer identity number is required")
                                       String customerIdentityNumber,

                                       @Future(message = "Pickup date must be in the future")
                                       @NotNull(message = "Pickup date is required")
                                       LocalDateTime pickupDateTime,
                                       Long insurancePackageId,
                                       @Future(message = "Return date must be in the future")
                                       @NotNull(message = "Return date is required")
                                       LocalDateTime returnDateTime) {
}
