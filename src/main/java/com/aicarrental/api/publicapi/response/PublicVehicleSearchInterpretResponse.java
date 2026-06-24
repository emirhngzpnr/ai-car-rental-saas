package com.aicarrental.api.publicapi.response;

import java.util.List;

public record PublicVehicleSearchInterpretResponse(
        PublicVehicleSearchCriteriaResponse criteria,
        PublicVehicleSearchInterpretationResponse interpretation,
        String summary,
        List<String> inferences,
        List<String> warnings
) {
}
