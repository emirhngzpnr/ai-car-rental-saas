package com.aicarrental.api.rental;

import com.aicarrental.api.rental.request.CompleteRentalRequest;
import com.aicarrental.api.rental.request.StartRentalRequest;
import com.aicarrental.api.rental.response.RentalResponse;
import com.aicarrental.application.rental.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
public class RentalController {
    private final RentalService rentalService;
    @PostMapping("/start")
    public ResponseEntity<RentalResponse> startRental(
            @Valid @RequestBody StartRentalRequest request
    ) {

        RentalResponse response =
                rentalService.startRental(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<RentalResponse> completeRental(
            @PathVariable Long id,
            @Valid @RequestBody CompleteRentalRequest request
    ) {

        RentalResponse response =
                rentalService.completeRental(id, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RentalResponse>> getAllRentals() {

        List<RentalResponse> response =
                rentalService.getAllRentals();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalResponse> getRentalById(
            @PathVariable Long id
    ) {

        RentalResponse response =
                rentalService.getRentalById(id);

        return ResponseEntity.ok(response);
    }
}
