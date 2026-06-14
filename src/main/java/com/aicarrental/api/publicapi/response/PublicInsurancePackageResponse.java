package com.aicarrental.api.publicapi.response;

import java.math.BigDecimal;

public record PublicInsurancePackageResponse(
        Long id,
        String type,
        String name,
        String coverageDescription,
        BigDecimal dailyPrice
) {
}
