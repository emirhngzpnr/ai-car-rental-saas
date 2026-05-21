package com.aicarrental.api.vehicle.response;
import com.aicarrental.api.vehicle.response.AvailableVehicleResponse;
import com.aicarrental.application.vehicle.VehicleAvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleAvailabilityController {
    private final VehicleAvailabilityService vehicleAvailabilityService;

    @GetMapping("/available")
    public ResponseEntity<List<AvailableVehicleResponse>> getAvailableVehicles(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime pickupDateTime,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime returnDateTime
    ) {
        return ResponseEntity.ok(
                vehicleAvailabilityService.searchAvailableVehicles(
                        pickupDateTime,
                        returnDateTime
                )
        );
    }
}
