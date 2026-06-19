package com.aicarrental.api.customer.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CustomerCreateReservationRequest(
        @NotBlank String tenantSlug,
        @NotNull Long vehicleId,
        @NotBlank String customerIdentityNumber,
        @Future @NotNull LocalDateTime pickupDateTime,
        @Future @NotNull LocalDateTime returnDateTime,
        Long insurancePackageId
) {
}
