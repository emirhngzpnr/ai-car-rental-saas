package com.aicarrental.api.publicapi.response;

import java.util.List;

public record PublicAssistantResponse(
        String answer,
        String intent,
        List<PublicAssistantCitationResponse> citations,
        PublicVehicleSearchCriteriaResponse vehicleSearchCriteria,
        PublicVehicleSearchDateCriteriaResponse dateCriteria,
        PublicMarketplaceSearchResponse vehicles,
        List<String> inferences,
        List<String> warnings
) {
}
