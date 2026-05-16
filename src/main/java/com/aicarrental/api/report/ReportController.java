package com.aicarrental.api.report;
import com.aicarrental.api.report.response.DashboardSummaryResponse;
import com.aicarrental.api.report.response.MonthlyRevenueResponse;
import com.aicarrental.api.report.response.MonthlySummaryResponse;
import com.aicarrental.api.report.response.TopVehicleResponse;
import com.aicarrental.application.report.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/dashboard-summary")
    public ResponseEntity<DashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(reportService.getDashboardSummary());
    }
    @GetMapping("/monthly-revenue")
    public ResponseEntity<List<MonthlyRevenueResponse>> getMonthlyRevenue() {
        return ResponseEntity.ok(reportService.getMonthlyRevenue());
    }
    @GetMapping("/monthly-summary")
    public ResponseEntity<MonthlySummaryResponse> getMonthlySummary(
            @RequestParam Integer year,
            @RequestParam Integer month
    ) {
        return ResponseEntity.ok(
                reportService.getMonthlySummary(year, month)
        );
    }
    @GetMapping("/top-vehicles")
    public ResponseEntity<List<TopVehicleResponse>> getTopVehicles(
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(reportService.getTopVehicles(limit));
    }
}
