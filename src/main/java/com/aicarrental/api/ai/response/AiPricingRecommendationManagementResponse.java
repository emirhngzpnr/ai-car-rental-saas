package com.aicarrental.api.ai.response;

import com.aicarrental.domain.ai.AiPricingRecommendationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AiPricingRecommendationManagementResponse(Long id,
                                                        Long tenantId,
                                                        Long vehicleId,
                                                        String plateNumber,
                                                        String brand,
                                                        String model,
                                                        BigDecimal currentPrice,
                                                        BigDecimal recommendedPrice,
                                                        String confidenceLevel,
                                                        String reason,
                                                        AiPricingRecommendationStatus status,
                                                        Long approvedByUserId,
                                                        Long rejectedByUserId,
                                                        LocalDateTime approvedAt,
                                                        LocalDateTime rejectedAt,
                                                        LocalDateTime createdAt) {
}
