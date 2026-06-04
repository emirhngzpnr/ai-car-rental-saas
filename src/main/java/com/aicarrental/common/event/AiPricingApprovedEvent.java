package com.aicarrental.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AiPricingApprovedEvent(Long recommendationId,
                                     Long tenantId,
                                     Long vehicleId,
                                     String plateNumber,
                                     String vehicleBrand,
                                     String vehicleModel,
                                     BigDecimal oldPrice,
                                     BigDecimal newPrice,
                                     Long approvedByUserId,
                                     String approvedByEmail,
                                     LocalDateTime approvedAt) {
}
