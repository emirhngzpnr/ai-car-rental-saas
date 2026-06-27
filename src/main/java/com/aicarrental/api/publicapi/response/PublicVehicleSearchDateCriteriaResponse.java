package com.aicarrental.api.publicapi.response;

import java.time.LocalDateTime;

public record PublicVehicleSearchDateCriteriaResponse(
        LocalDateTime pickupDateTime,
        LocalDateTime returnDateTime
) {
}
