package com.aicarrental.api.report.response;

import java.math.BigDecimal;

public record MonthlySummaryResponse(Integer year,
                                     Integer month,
                                     BigDecimal totalRevenue,
                                     Long completedRentals,
                                     BigDecimal refundAmount,
                                     BigDecimal extraKmRevenue,
                                     Long issuedInvoices) {
}
