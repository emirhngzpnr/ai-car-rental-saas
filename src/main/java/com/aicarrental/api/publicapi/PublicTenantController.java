package com.aicarrental.api.publicapi;

import com.aicarrental.api.publicapi.request.PublicCreateReservationRequest;
import com.aicarrental.api.publicapi.request.PublicDepositPaymentRequest;
import com.aicarrental.api.publicapi.response.PublicAvailableVehicleResponse;
import com.aicarrental.api.publicapi.response.PublicDepositPaymentResponse;
import com.aicarrental.api.publicapi.response.PublicReservationResponse;
import com.aicarrental.api.publicapi.response.PublicTenantResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleDetailResponse;
import com.aicarrental.application.publicapi.PublicPaymentService;
import com.aicarrental.application.publicapi.PublicReservationService;
import com.aicarrental.application.publicapi.PublicTenantService;
import com.aicarrental.application.publicapi.PublicVehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/public/tenants/{tenantSlug}")
@RequiredArgsConstructor
public class PublicTenantController {
    private final PublicTenantService publicTenantService;
    private final PublicVehicleService publicVehicleService;
    private final PublicReservationService publicReservationService;
    private final PublicPaymentService publicPaymentService;

    @GetMapping
    public ResponseEntity<PublicTenantResponse> getTenant(
            @PathVariable String tenantSlug
    ) {
        return ResponseEntity.ok(publicTenantService.getTenant(tenantSlug));
    }

    @GetMapping("/vehicles/available")
    public ResponseEntity<List<PublicAvailableVehicleResponse>> getAvailableVehicles(
            @PathVariable String tenantSlug,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime pickupDateTime,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime returnDateTime
    ) {
        return ResponseEntity.ok(
                publicVehicleService.getAvailableVehicles(
                        tenantSlug,
                        pickupDateTime,
                        returnDateTime
                )
        );
    }

    @GetMapping("/vehicles/{vehicleId}")
    public ResponseEntity<PublicVehicleDetailResponse> getVehicleDetail(
            @PathVariable String tenantSlug,
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(
                publicVehicleService.getVehicleDetail(tenantSlug, vehicleId)
        );
    }

    @PostMapping("/reservations")
    public ResponseEntity<PublicReservationResponse> createReservation(
            @PathVariable String tenantSlug,
            @Valid @RequestBody PublicCreateReservationRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(publicReservationService.createReservation(tenantSlug, request));
    }

    @PostMapping("/reservations/{reservationCode}/deposit-payment")
    public ResponseEntity<PublicDepositPaymentResponse> payDeposit(
            @PathVariable String tenantSlug,
            @PathVariable String reservationCode,
            @Valid @RequestBody PublicDepositPaymentRequest request
    ) {
        return ResponseEntity.ok(
                publicPaymentService.payDeposit(tenantSlug, reservationCode, request)
        );
    }
}
