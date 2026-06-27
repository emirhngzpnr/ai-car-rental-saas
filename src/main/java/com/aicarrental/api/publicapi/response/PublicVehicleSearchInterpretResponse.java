package com.aicarrental.api.publicapi.response;

import java.util.List;

public record PublicVehicleSearchInterpretResponse(
        PublicVehicleSearchCriteriaResponse criteria,
        PublicVehicleSearchDateCriteriaResponse dateCriteria,
        PublicVehicleSearchInterpretationResponse interpretation,
        List<String> missingFields,
        String summary,
        List<String> inferences,
        List<String> warnings
) {
}
