package com.aicarrental.api.report.response;

import java.math.BigDecimal;

public record MonthlyRevenueResponse(String month,
                                     BigDecimal totalRevenue
) {
}
