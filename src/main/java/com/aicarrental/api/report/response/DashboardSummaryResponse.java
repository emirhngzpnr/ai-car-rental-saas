package com.aicarrental.api.report.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(Long activeRentalsCount,
                                       Long completedRentalsCount,
                                       BigDecimal totalRevenue,
                                       BigDecimal totalRefundAmount,
                                       BigDecimal totalInvoiceAmount,
                                       BigDecimal totalExtraKmRevenue,
                                       Long pendingPaymentsCount,
                                       Long availableVehiclesCount) {
}
