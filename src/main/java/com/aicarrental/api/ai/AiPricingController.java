package com.aicarrental.api.ai;
import com.aicarrental.api.ai.response.AiPricingRecommendationResponse;
import com.aicarrental.application.ai.AiPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/pricing")
@RequiredArgsConstructor
public class AiPricingController {
    private final AiPricingService aiPricingService;

    @GetMapping("/recommendation/{vehicleId}")
    public ResponseEntity<AiPricingRecommendationResponse> recommendPrice(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(
                aiPricingService.recommendPrice(vehicleId)
        );
    }
}
