package com.aicarrental.application.report;
import com.aicarrental.api.report.response.DashboardSummaryResponse;
import com.aicarrental.api.report.response.MonthlyRevenueResponse;
import com.aicarrental.api.report.response.MonthlySummaryResponse;
import com.aicarrental.api.report.response.TopVehicleResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.invoice.InvoiceStatus;
import com.aicarrental.domain.payment.PaymentStatus;
import com.aicarrental.domain.payment.PaymentType;
import com.aicarrental.domain.rental.RentalStatus;
import com.aicarrental.domain.vehicle.VehicleStatus;
import com.aicarrental.infrastructure.persistence.InvoiceRepository;
import com.aicarrental.infrastructure.persistence.PaymentTransactionRepository;
import com.aicarrental.infrastructure.persistence.RentalRepository;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import com.aicarrental.infrastructure.persistence.projection.MonthlyRevenueProjection;
import com.aicarrental.infrastructure.persistence.projection.TopVehicleProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final RentalRepository rentalRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;

    public DashboardSummaryResponse getDashboardSummary() {

        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isSuperAdmin(currentUser)) {
            return getSuperAdminDashboardSummary();
        }

        Long tenantId = currentUserService.getCurrentTenantId();

        return getTenantDashboardSummary(tenantId);
    }

    private DashboardSummaryResponse getTenantDashboardSummary(Long tenantId) {

        Long activeRentalsCount =
                rentalRepository.countByTenant_IdAndStatus(
                        tenantId,
                        RentalStatus.ACTIVE
                );

        Long completedRentalsCount =
                rentalRepository.countByTenant_IdAndStatus(
                        tenantId,
                        RentalStatus.COMPLETED
                );

        BigDecimal totalRevenue =
                rentalRepository.sumFinalRentalPriceByTenantAndStatus(
                        tenantId,
                        RentalStatus.COMPLETED
                );

        BigDecimal totalRefundAmount =
                paymentTransactionRepository.sumAmountByTenantAndTypeAndStatus(
                        tenantId,
                        PaymentType.REFUND,
                        PaymentStatus.REFUNDED
                );

        BigDecimal totalInvoiceAmount =
                invoiceRepository.sumTotalAmountByTenantAndStatus(
                        tenantId,
                        InvoiceStatus.ISSUED
                );

        BigDecimal totalExtraKmRevenue =
                rentalRepository.sumExtraKmFeeByTenantAndStatus(
                        tenantId,
                        RentalStatus.COMPLETED
                );

        Long pendingPaymentsCount =
                paymentTransactionRepository.countByTenant_IdAndPaymentStatus(
                        tenantId,
                        PaymentStatus.PENDING
                );

        Long availableVehiclesCount =
                vehicleRepository.countByTenant_IdAndStatusAndActiveTrue(
                        tenantId,
                        VehicleStatus.AVAILABLE
                );

        return new DashboardSummaryResponse(
                activeRentalsCount,
                completedRentalsCount,
                totalRevenue,
                totalRefundAmount,
                totalInvoiceAmount,
                totalExtraKmRevenue,
                pendingPaymentsCount,
                availableVehiclesCount
        );
    }

    private DashboardSummaryResponse getSuperAdminDashboardSummary() {

        Long activeRentalsCount =
                rentalRepository.countByStatus(RentalStatus.ACTIVE);

        Long completedRentalsCount =
                rentalRepository.countByStatus(RentalStatus.COMPLETED);

        BigDecimal totalRevenue =
                rentalRepository.sumFinalRentalPriceByStatus(
                        RentalStatus.COMPLETED
                );

        BigDecimal totalRefundAmount =
                paymentTransactionRepository.sumAmountByTypeAndStatus(
                        PaymentType.REFUND,
                        PaymentStatus.REFUNDED
                );

        BigDecimal totalInvoiceAmount =
                invoiceRepository.sumTotalAmountByStatus(
                        InvoiceStatus.ISSUED
                );

        BigDecimal totalExtraKmRevenue =
                rentalRepository.sumExtraKmFeeByStatus(
                        RentalStatus.COMPLETED
                );

        Long pendingPaymentsCount =
                paymentTransactionRepository.countByPaymentStatus(
                        PaymentStatus.PENDING
                );

        Long availableVehiclesCount =
                vehicleRepository.countByStatusAndActiveTrue(
                        VehicleStatus.AVAILABLE
                );

        return new DashboardSummaryResponse(
                activeRentalsCount,
                completedRentalsCount,
                totalRevenue,
                totalRefundAmount,
                totalInvoiceAmount,
                totalExtraKmRevenue,
                pendingPaymentsCount,
                availableVehiclesCount
        );
    }
    public List<MonthlyRevenueResponse> getMonthlyRevenue() {
        User currentUser = currentUserService.getCurrentUser();

        List<MonthlyRevenueProjection> projections;

        if (currentUserService.isSuperAdmin(currentUser)) {
            projections = invoiceRepository.getMonthlyRevenue();
        } else {
            Long tenantId = currentUserService.getCurrentTenantId();
            projections = invoiceRepository.getMonthlyRevenueByTenant(tenantId);
        }

        return projections.stream()
                .map(item -> new MonthlyRevenueResponse(
                        item.getMonth(),
                        item.getTotalRevenue()
                ))
                .toList();
    }
    public MonthlySummaryResponse getMonthlySummary(
            Integer year,
            Integer month
    ) {
        if (year == null || month == null) {
            throw new BusinessException("Year and month are required");
        }

        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12");
        }

        if (year < 2026) {
            throw new BusinessException("Reports before 2026 are not supported");
        }

        YearMonth requestedMonth = YearMonth.of(year, month);
        YearMonth currentMonth = YearMonth.now();

        if (requestedMonth.isAfter(currentMonth)) {
            throw new BusinessException("Future months cannot be queried");
        }

        User currentUser = currentUserService.getCurrentUser();

        if (currentUserService.isSuperAdmin(currentUser)) {
            return getSuperAdminMonthlySummary(year, month);
        }

        Long tenantId = currentUserService.getCurrentTenantId();

        return getTenantMonthlySummary(tenantId, year, month);
    }
    private MonthlySummaryResponse getTenantMonthlySummary(
            Long tenantId,
            Integer year,
            Integer month
    ) {
        BigDecimal totalRevenue =
                invoiceRepository.sumMonthlyInvoiceAmountByTenant(
                        tenantId,
                        year,
                        month
                );

        Long completedRentals =
                rentalRepository.countMonthlyCompletedRentalsByTenant(
                        tenantId,
                        year,
                        month
                );

        BigDecimal refundAmount =
                paymentTransactionRepository.sumMonthlyRefundAmountByTenant(
                        tenantId,
                        year,
                        month
                );

        BigDecimal extraKmRevenue =
                rentalRepository.sumMonthlyExtraKmRevenueByTenant(
                        tenantId,
                        year,
                        month
                );

        Long issuedInvoices =
                invoiceRepository.countMonthlyIssuedInvoicesByTenant(
                        tenantId,
                        year,
                        month
                );

        return new MonthlySummaryResponse(
                year,
                month,
                totalRevenue,
                completedRentals,
                refundAmount,
                extraKmRevenue,
                issuedInvoices
        );
    }
    private MonthlySummaryResponse getSuperAdminMonthlySummary(
            Integer year,
            Integer month
    ) {
        BigDecimal totalRevenue =
                invoiceRepository.sumMonthlyInvoiceAmount(
                        year,
                        month
                );

        Long completedRentals =
                rentalRepository.countMonthlyCompletedRentals(
                        year,
                        month
                );

        BigDecimal refundAmount =
                paymentTransactionRepository.sumMonthlyRefundAmount(
                        year,
                        month
                );

        BigDecimal extraKmRevenue =
                rentalRepository.sumMonthlyExtraKmRevenue(
                        year,
                        month
                );

        Long issuedInvoices =
                invoiceRepository.countMonthlyIssuedInvoices(
                        year,
                        month
                );

        return new MonthlySummaryResponse(
                year,
                month,
                totalRevenue,
                completedRentals,
                refundAmount,
                extraKmRevenue,
                issuedInvoices
        );
    }
    public List<TopVehicleResponse> getTopVehicles(Integer limit) {
        int safeLimit = limit == null ? 5 : limit;

        if (safeLimit < 1 || safeLimit > 50) {
            throw new BusinessException("Limit must be between 1 and 50");
        }

        User currentUser = currentUserService.getCurrentUser();

        List<TopVehicleProjection> projections;

        if (currentUserService.isSuperAdmin(currentUser)) {
            projections = rentalRepository.findTopVehicles(safeLimit);
        } else {
            Long tenantId = currentUserService.getCurrentTenantId();
            projections = rentalRepository.findTopVehiclesByTenant(tenantId, safeLimit);
        }

        return projections.stream()
                .map(item -> new TopVehicleResponse(
                        item.getVehicleId(),
                        item.getPlateNumber(),
                        item.getBrand(),
                        item.getModel(),
                        item.getRentalCount(),
                        item.getTotalRevenue()
                ))
                .toList();
    }
}
