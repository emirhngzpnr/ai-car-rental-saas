package com.aicarrental.api.publicapi;

import com.aicarrental.api.publicapi.request.PublicVehicleSearchInterpretRequest;
import com.aicarrental.api.publicapi.response.PublicVehicleSearchInterpretResponse;
import com.aicarrental.application.publicapi.PublicVehicleSearchInterpretationService;
import com.aicarrental.infrastructure.ratelimit.PublicAiRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/marketplace/vehicle-search")
@RequiredArgsConstructor
public class PublicMarketplaceAiController {
    private final PublicVehicleSearchInterpretationService interpretationService;
    private final PublicAiRateLimiter rateLimiter;

    @PostMapping("/interpret")
    public ResponseEntity<PublicVehicleSearchInterpretResponse> interpret(
            @Valid @RequestBody PublicVehicleSearchInterpretRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimiter.checkAllowed(servletRequest.getRemoteAddr());
        return ResponseEntity.ok(interpretationService.interpret(
                request.query(),
                request.pickupDateTime(),
                request.returnDateTime(),
                request.location()
        ));
    }
}
