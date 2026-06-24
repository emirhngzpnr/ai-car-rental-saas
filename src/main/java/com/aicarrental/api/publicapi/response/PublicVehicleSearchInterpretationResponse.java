package com.aicarrental.api.publicapi.response;

import com.aicarrental.application.publicapi.ai.PriceIntent;
import com.aicarrental.application.publicapi.ai.SegmentIntent;

public record PublicVehicleSearchInterpretationResponse(
        PriceIntent priceIntent,
        SegmentIntent segmentIntent
) {
}
