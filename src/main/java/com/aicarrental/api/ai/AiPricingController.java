package com.aicarrental.api.ai;
import com.aicarrental.api.ai.response.AiPricingRecommendationManagementResponse;
import com.aicarrental.api.ai.response.AiPricingRecommendationResponse;
import com.aicarrental.application.ai.AiPricingService;
import com.aicarrental.domain.ai.AiPricingRecommendationStatus;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    // TODO: Refactor to @ParameterObject when Swagger/Springdoc issue is resolved//    @GetMapping("/recommendations")
//    public ResponseEntity<Page<AiPricingRecommendationManagementResponse>> getRecommendations(
//            @RequestParam(required = false) AiPricingRecommendationStatus status,
//            @RequestParam(required = false) Long vehicleId,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime createdAfter,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime createdBefore,
//            @ParameterObject
//            @PageableDefault(
//                    page = 0,
//                    size = 10,
//                    sort = "createdAt",
//                    direction = Sort.Direction.DESC
//            )
//            Pageable pageable
//    ) {
//        return ResponseEntity.ok(
//                aiPricingService.getRecommendations(
//                        status,
//                        vehicleId,
//                        createdAfter,
//                        createdBefore,
//                        pageable
//                )
//        );
//    }
@GetMapping("/recommendations")
public ResponseEntity<Page<AiPricingRecommendationManagementResponse>> getRecommendations(
        @RequestParam(required = false) AiPricingRecommendationStatus status,
        @RequestParam(required = false) Long vehicleId,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdAfter,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime createdBefore,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
) {
    Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.DESC, "createdAt")
    );

    return ResponseEntity.ok(
            aiPricingService.getRecommendations(
                    status,
                    vehicleId,
                    createdAfter,
                    createdBefore,
                    pageable
            )
    );
}
}
