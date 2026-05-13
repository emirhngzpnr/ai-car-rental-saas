package com.aicarrental.api.insurance.response;

import com.aicarrental.domain.insurance.InsurancePackageType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InsurancePackageResponse(Long id,
                                       Long tenantId,
                                       String tenantName,
                                       InsurancePackageType type,
                                       String name,
                                       String coverageDescription,
                                       BigDecimal dailyPrice,
                                       Boolean active,
                                       LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {
}
