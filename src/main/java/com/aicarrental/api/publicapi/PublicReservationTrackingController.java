package com.aicarrental.api.publicapi;

import com.aicarrental.api.publicapi.response.PublicReservationTrackingResponse;
import com.aicarrental.application.publicapi.PublicReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/reservations")
@RequiredArgsConstructor
public class PublicReservationTrackingController {
    private final PublicReservationService publicReservationService;

    @GetMapping("/track")
    public ResponseEntity<PublicReservationTrackingResponse> trackReservation(
            @RequestParam String reservationCode,
            @RequestParam String email
    ) {
        return ResponseEntity.ok(
                publicReservationService.trackReservation(reservationCode, email)
        );
    }
}
