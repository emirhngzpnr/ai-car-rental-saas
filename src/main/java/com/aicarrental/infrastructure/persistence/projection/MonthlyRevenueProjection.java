package com.aicarrental.infrastructure.persistence.projection;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
@Repository
public interface MonthlyRevenueProjection {
    String getMonth();

    BigDecimal getTotalRevenue();
}
