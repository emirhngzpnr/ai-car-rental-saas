package com.aicarrental.api.ai;
import com.aicarrental.api.ai.response.AiPricingRecommendationManagementResponse;
import com.aicarrental.api.ai.response.AiPricingRecommendationResponse;
import com.aicarrental.application.ai.AiPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping("/recommendations/pending")
    public ResponseEntity<List<AiPricingRecommendationManagementResponse>> getPendingRecommendations() {
        return ResponseEntity.ok(
                aiPricingService.getPendingRecommendations()
        );
    }
    @PostMapping("/recommendations/{id}/approve")
    public ResponseEntity<AiPricingRecommendationManagementResponse> approveRecommendation(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                aiPricingService.approveRecommendation(id)
        );
    }
    @PostMapping("/recommendations/{id}/reject")
    public ResponseEntity<AiPricingRecommendationManagementResponse> rejectRecommendation(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                aiPricingService.rejectRecommendation(id)
        );
    }
}
