package com.aicarrental.api.publicapi;

import com.aicarrental.api.publicapi.response.PublicMarketplaceSearchResponse;
import com.aicarrental.api.publicapi.response.PublicMarketplaceVehicleDetailResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleReviewPageResponse;
import com.aicarrental.application.publicapi.PublicMarketplaceService;
import com.aicarrental.application.publicapi.PublicVehicleReviewService;
import com.aicarrental.domain.vehicle.FuelType;
import com.aicarrental.domain.vehicle.TransmissionType;
import com.aicarrental.domain.vehicle.VehicleCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/public/marketplace/vehicles")
@RequiredArgsConstructor
public class PublicMarketplaceController {
    private final PublicMarketplaceService marketplaceService;
    private final PublicVehicleReviewService reviewService;

    @GetMapping
    public ResponseEntity<PublicMarketplaceSearchResponse> search(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pickupDateTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime returnDateTime,
            @RequestParam(required = false) BigDecimal minDailyPrice,
            @RequestParam(required = false) BigDecimal maxDailyPrice,
            @RequestParam(required = false) Integer minDailyKmLimit,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) VehicleCategory category,
            @RequestParam(required = false) List<VehicleCategory> categories,
            @RequestParam(required = false) TransmissionType transmission,
            @RequestParam(required = false) FuelType fuelType,
            @RequestParam(required = false) Integer minSeats,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "recommended") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ResponseEntity.ok(marketplaceService.search(
                pickupDateTime, returnDateTime, minDailyPrice, maxDailyPrice,
                minDailyKmLimit, brand, model, category, categories,
                transmission, fuelType, minSeats, location, sort, page, size
        ));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<PublicMarketplaceVehicleDetailResponse> getVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(marketplaceService.getVehicle(vehicleId));
    }

    @GetMapping("/{vehicleId}/reviews")
    public ResponseEntity<PublicVehicleReviewPageResponse> getReviews(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(reviewService.getReviews(vehicleId, page, size));
    }
}
