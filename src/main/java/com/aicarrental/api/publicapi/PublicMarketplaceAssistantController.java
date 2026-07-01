package com.aicarrental.api.publicapi;

import com.aicarrental.api.publicapi.request.PublicAssistantQueryRequest;
import com.aicarrental.api.publicapi.response.PublicAssistantResponse;
import com.aicarrental.application.publicapi.assistant.PublicMarketplaceAssistantService;
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
@RequestMapping("/api/public/marketplace/assistant")
@RequiredArgsConstructor
public class PublicMarketplaceAssistantController {
    private final PublicMarketplaceAssistantService assistantService;
    private final PublicAiRateLimiter rateLimiter;

    @PostMapping("/query")
    public ResponseEntity<PublicAssistantResponse> query(
            @Valid @RequestBody PublicAssistantQueryRequest request,
            HttpServletRequest servletRequest
    ) {
        rateLimiter.checkAllowed(servletRequest.getRemoteAddr());
        return ResponseEntity.ok(assistantService.query(request));
    }
}
