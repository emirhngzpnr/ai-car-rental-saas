package com.aicarrental.api.ai.response;

import java.math.BigDecimal;

public record AiPricingRecommendationResponse(Long vehicleId,
                                              BigDecimal currentDailyPrice,
                                              BigDecimal recommendedDailyPrice,
                                              String confidenceLevel,
                                              String reason) {
}
