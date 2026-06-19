package com.aicarrental.api.customer;

import com.aicarrental.api.customer.request.CustomerCreateReservationRequest;
import com.aicarrental.api.customer.request.CustomerDepositPaymentRequest;
import com.aicarrental.api.customer.request.CustomerProfileUpdateRequest;
import com.aicarrental.api.customer.response.CustomerProfileResponse;
import com.aicarrental.api.customer.response.CustomerReservationResponse;
import com.aicarrental.api.publicapi.response.PublicDepositPaymentResponse;
import com.aicarrental.api.publicapi.response.PublicReservationResponse;
import com.aicarrental.application.customer.CustomerProfileService;
import com.aicarrental.application.customer.CustomerReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerAccountController {
    private final CustomerProfileService profileService;
    private final CustomerReservationService reservationService;

    @GetMapping("/me")
    public ResponseEntity<CustomerProfileResponse> profile() {
        return ResponseEntity.ok(profileService.getProfile());
    }

    @PutMapping("/me")
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            @Valid @RequestBody CustomerProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(profileService.updateProfile(request));
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<CustomerReservationResponse>> reservations() {
        return ResponseEntity.ok(reservationService.getReservations());
    }

    @GetMapping("/reservations/{reservationCode}")
    public ResponseEntity<CustomerReservationResponse> reservation(@PathVariable String reservationCode) {
        return ResponseEntity.ok(reservationService.getReservation(reservationCode));
    }

    @PostMapping("/reservations")
    public ResponseEntity<PublicReservationResponse> createReservation(
            @Valid @RequestBody CustomerCreateReservationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.create(request));
    }

    @PostMapping("/reservations/{reservationCode}/deposit-payment")
    public ResponseEntity<PublicDepositPaymentResponse> payDeposit(
            @PathVariable String reservationCode,
            @Valid @RequestBody CustomerDepositPaymentRequest request
    ) {
        return ResponseEntity.ok(reservationService.payDeposit(reservationCode, request.idempotencyKey()));
    }
}
