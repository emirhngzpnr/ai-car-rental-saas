package com.aicarrental.api.publicapi.response;

import java.util.List;

public record PublicVehicleSearchInterpretResponse(
        PublicVehicleSearchCriteriaResponse criteria,
        String summary,
        List<String> warnings
) {
}
