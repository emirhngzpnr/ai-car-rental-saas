package com.aicarrental.application.ai;
import com.aicarrental.api.ai.response.AiPricingRecommendationManagementResponse;
import com.aicarrental.api.ai.response.AiPricingRecommendationResponse;
import com.aicarrental.application.ai.context.AiPricingContext;
import com.aicarrental.application.ai.specification.AiPricingRecommendationSpecification;
import com.aicarrental.application.tenant.TenantSettingService;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.event.AiPricingApprovedEvent;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.ai.AiPricingRecommendation;
import com.aicarrental.domain.ai.AiPricingRecommendationStatus;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.outbox.OutboxEventType;
import com.aicarrental.domain.outbox.OutboxMessage;
import com.aicarrental.domain.outbox.OutboxMessageStatus;
import com.aicarrental.domain.tenant.TenantSettingKey;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.infrastructure.persistence.AiPricingRecommendationRepository;
import com.aicarrental.infrastructure.persistence.OutboxMessageRepository;
import com.aicarrental.infrastructure.persistence.RentalRepository;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import com.aicarrental.infrastructure.persistence.projection.AiPricingProjection;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiPricingService {
    private final RentalRepository rentalRepository;
    private final TenantSettingService tenantSettingService;
    private final CurrentUserService currentUserService;
    private final ChatClient.Builder chatClientBuilder;
    private final AiPricingRecommendationRepository aiPricingRecommendationRepository;
    private final VehicleRepository vehicleRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;

    public AiPricingRecommendationResponse recommendPrice(Long vehicleId) {

        Long tenantId = currentUserService.getCurrentTenantId();

        validateAiPricingEnabled();

        AiPricingContext context = buildPricingContext(vehicleId, tenantId);

        AiPricingRecommendationResponse recommendation;

        try {
            recommendation = callAiModel(context);

        } catch (Exception exception) {

            log.error(
                    "AI pricing model call failed. Falling back to rule-based recommendation",
                    exception
            );

            recommendation = fallbackRecommendation(context);
        }

        saveRecommendation(
                tenantId,
                vehicleId,
                recommendation
        );

        return recommendation;
    }
    private void validateAiPricingEnabled() {

        boolean enabled =
                tenantSettingService.getCurrentTenantSettings()
                        .stream()
                        .filter(setting ->
                                setting.settingKey().equals(
                                        TenantSettingKey.AI_PRICING_ENABLED.name()
                                )
                        )
                        .findFirst()
                        .map(setting ->
                                Boolean.parseBoolean(setting.settingValue())
                        )
                        .orElse(false);

        if (!enabled) {
            throw new BusinessException("AI pricing is disabled for this tenant");
        }
    }
    private AiPricingContext buildPricingContext(
            Long vehicleId,
            Long tenantId
    ) {
        AiPricingProjection projection =
                rentalRepository.getAiPricingAnalytics(vehicleId, tenantId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Vehicle not found")
                        );

        return new AiPricingContext(
                projection.getVehicleId(),
                projection.getPlateNumber(),
                projection.getBrand(),
                projection.getModel(),
                projection.getCurrentDailyPrice(),
                projection.getCompletedRentalsCount(),
                projection.getTotalRevenue(),
                projection.getExtraKmRevenue(),
                projection.getRefundAmount()
        );
    }

    private AiPricingRecommendationResponse callAiModel(
            AiPricingContext context
    ) {
        ChatClient chatClient = chatClientBuilder.build();

        String prompt = """
                You are an AI pricing analyst for a multi-tenant vehicle rental SaaS.

                Analyze the vehicle performance data and recommend a daily rental price.

                Rules:
                - Return ONLY valid JSON.
                - Do not include markdown.
                - recommendedDailyPrice must be a positive number.
                - confidenceLevel must be one of: LOW, MEDIUM, HIGH.
                - Reason must be concise and business-oriented.
                - Do not recommend more than 30 percent increase or more than 20 percent decrease from currentDailyPrice.

                Vehicle data:
                vehicleId: %d
                plateNumber: %s
                brand: %s
                model: %s
                currentDailyPrice: %s
                completedRentalsCount: %d
                totalRevenue: %s
                extraKmRevenue: %s
                refundAmount: %s

                Expected JSON schema:
                {
                  "vehicleId": 1,
                  "currentDailyPrice": 1500.00,
                  "recommendedDailyPrice": 1650.00,
                  "confidenceLevel": "MEDIUM",
                  "reason": "Short business explanation"
                }
                """.formatted(
                context.vehicleId(),
                context.plateNumber(),
                context.brand(),
                context.model(),
                context.currentDailyPrice(),
                context.completedRentalsCount(),
                context.totalRevenue(),
                context.extraKmRevenue(),
                context.refundAmount()
        );

        AiPricingRecommendationResponse response =
                chatClient.prompt()
                        .user(prompt)
                        .call()
                        .entity(AiPricingRecommendationResponse.class);

        return validateAndNormalizeAiResponse(response, context);
    }

    private AiPricingRecommendationResponse validateAndNormalizeAiResponse(
            AiPricingRecommendationResponse response,
            AiPricingContext context
    ) {
        if (response == null) {
            return fallbackRecommendation(context);
        }

        if (response.recommendedDailyPrice() == null ||
                response.recommendedDailyPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return fallbackRecommendation(context);
        }

        BigDecimal currentPrice = context.currentDailyPrice();

        BigDecimal maxAllowed =
                currentPrice.multiply(BigDecimal.valueOf(1.30));

        BigDecimal minAllowed =
                currentPrice.multiply(BigDecimal.valueOf(0.80));

        BigDecimal recommended =
                response.recommendedDailyPrice();

        if (recommended.compareTo(maxAllowed) > 0) {
            recommended = maxAllowed;
        }

        if (recommended.compareTo(minAllowed) < 0) {
            recommended = minAllowed;
        }

        recommended = recommended.setScale(2, RoundingMode.HALF_UP);

        return new AiPricingRecommendationResponse(
                context.vehicleId(),
                currentPrice,
                recommended,
                normalizeConfidence(response.confidenceLevel()),
                response.reason() != null
                        ? response.reason()
                        : "AI generated a pricing recommendation based on rental performance."
        );
    }

    private String normalizeConfidence(String confidenceLevel) {
        if (confidenceLevel == null) {
            return "LOW";
        }

        return switch (confidenceLevel.toUpperCase()) {
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private AiPricingRecommendationResponse fallbackRecommendation(
            AiPricingContext context
    ) {
        BigDecimal currentPrice = context.currentDailyPrice();

        BigDecimal recommended = currentPrice;

        if (context.completedRentalsCount() != null &&
                context.completedRentalsCount() >= 5) {
            recommended = currentPrice.multiply(BigDecimal.valueOf(1.10));
        }

        recommended = recommended.setScale(2, RoundingMode.HALF_UP);

        return new AiPricingRecommendationResponse(
                context.vehicleId(),
                currentPrice,
                recommended,
                "LOW",
                "Fallback recommendation generated from historical rental count."
        );
    }
    private void saveRecommendation(
            Long tenantId,
            Long vehicleId,
            AiPricingRecommendationResponse response
    ) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Vehicle not found")
                );

        AiPricingRecommendation recommendation =
                AiPricingRecommendation.builder()
                        .tenant(vehicle.getTenant())
                        .vehicle(vehicle)
                        .currentPrice(response.currentDailyPrice())
                        .recommendedPrice(response.recommendedDailyPrice())
                        .confidenceLevel(response.confidenceLevel())
                        .reason(response.reason())
                        .status(AiPricingRecommendationStatus.PENDING)
                        .build();

        aiPricingRecommendationRepository.save(recommendation);
    }
    public List<AiPricingRecommendationManagementResponse> getPendingRecommendations() {

        User currentUser = currentUserService.getCurrentUser();

        List<AiPricingRecommendation> recommendations;

        if (currentUserService.isSuperAdmin(currentUser)) {
            recommendations =
                    aiPricingRecommendationRepository
                            .findByStatusOrderByCreatedAtDesc(
                                    AiPricingRecommendationStatus.PENDING
                            );
        } else {
            Long tenantId = currentUserService.getCurrentTenantId();

            recommendations =
                    aiPricingRecommendationRepository
                            .findByTenant_IdAndStatusOrderByCreatedAtDesc(
                                    tenantId,
                                    AiPricingRecommendationStatus.PENDING
                            );
        }

        return recommendations.stream()
                .map(this::mapToManagementResponse)
                .toList();
    }
    public AiPricingRecommendationManagementResponse approveRecommendation(
            Long recommendationId
    ) {
        User currentUser = currentUserService.getCurrentUser();

        AiPricingRecommendation recommendation =
                aiPricingRecommendationRepository.findById(recommendationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("AI pricing recommendation not found")
                        );

        if (recommendation.getStatus() != AiPricingRecommendationStatus.PENDING) {
            throw new BusinessException("Only pending recommendations can be approved");
        }

        if (!currentUserService.isSuperAdmin(currentUser)
                && !recommendation.getTenant().getId().equals(currentUserService.getCurrentTenantId())) {
            throw new BusinessException("You cannot approve this recommendation");
        }

        Vehicle vehicle = recommendation.getVehicle();

        vehicle.setDailyPrice(recommendation.getRecommendedPrice());
        vehicle.setUpdatedAt(LocalDateTime.now());

        vehicleRepository.save(vehicle);

        recommendation.setStatus(AiPricingRecommendationStatus.APPROVED);
        recommendation.setApprovedBy(currentUser);
        recommendation.setApprovedAt(LocalDateTime.now());
        BigDecimal oldPrice = vehicle.getDailyPrice();
        AiPricingRecommendation saved =
                aiPricingRecommendationRepository.save(recommendation);
        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                recommendation.getTenant().getId(),
                AuditAction.AI_PRICING_RECOMMENDATION_APPROVED,
                "AiPricingRecommendation",
                saved.getId(),
                "AI pricing recommendation approved. VehicleId: "
                        + vehicle.getId()
                        + ", Old price: "
                        + oldPrice
                        + ", New price: "
                        + vehicle.getDailyPrice()
        ));
        AiPricingApprovedEvent event = new AiPricingApprovedEvent(
                saved.getId(),
                recommendation.getTenant().getId(),
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getBrand(),
                vehicle.getModel(),
                oldPrice,
                vehicle.getDailyPrice(),
                currentUser.getId(),
                currentUser.getEmail(),
                LocalDateTime.now()
        );

        try {
            outboxMessageRepository.save(
                    OutboxMessage.builder()
                            .eventType(OutboxEventType.AI_PRICING_APPROVED)
                            .topic("ai-pricing-approved")
                            .messageKey(saved.getId().toString())
                            .payload(objectMapper.writeValueAsString(event))
                            .status(OutboxMessageStatus.PENDING)
                            .retryCount(0)
                            .build()
            );
        } catch (Exception exception) {
            throw new BusinessException("Failed to create AI pricing approved event");
        }
        return mapToManagementResponse(saved);
    }
    public AiPricingRecommendationManagementResponse rejectRecommendation(
            Long recommendationId
    ) {
        User currentUser = currentUserService.getCurrentUser();

        AiPricingRecommendation recommendation =
                aiPricingRecommendationRepository.findById(recommendationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("AI pricing recommendation not found")
                        );

        if (recommendation.getStatus() != AiPricingRecommendationStatus.PENDING) {
            throw new BusinessException("Only pending recommendations can be rejected");
        }

        if (!currentUserService.isSuperAdmin(currentUser)
                && !recommendation.getTenant().getId().equals(currentUserService.getCurrentTenantId())) {
            throw new BusinessException("You cannot reject this recommendation");
        }

        recommendation.setStatus(AiPricingRecommendationStatus.REJECTED);
        recommendation.setRejectedBy(currentUser);
        recommendation.setRejectedAt(LocalDateTime.now());

        AiPricingRecommendation saved =
                aiPricingRecommendationRepository.save(recommendation);

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                recommendation.getTenant().getId(),
                AuditAction.AI_PRICING_RECOMMENDATION_REJECTED,
                "AiPricingRecommendation",
                saved.getId(),
                "AI pricing recommendation rejected. VehicleId: "
                        + recommendation.getVehicle().getId()
                        + ", Current price: "
                        + recommendation.getCurrentPrice()
                        + ", Recommended price: "
                        + recommendation.getRecommendedPrice()
        ));

        return mapToManagementResponse(saved);
    }
    public Page<AiPricingRecommendationManagementResponse> getRecommendations(
            AiPricingRecommendationStatus status,
            Long vehicleId,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable
    ) {
        User currentUser = currentUserService.getCurrentUser();

        Long tenantId = currentUserService.isSuperAdmin(currentUser)
                ? null
                : currentUserService.getCurrentTenantId();

        Specification<AiPricingRecommendation> specification =
                Specification
                        .where(AiPricingRecommendationSpecification.hasTenantId(tenantId))
                        .and(AiPricingRecommendationSpecification.hasStatus(status))
                        .and(AiPricingRecommendationSpecification.hasVehicleId(vehicleId))
                        .and(AiPricingRecommendationSpecification.createdAfter(createdAfter))
                        .and(AiPricingRecommendationSpecification.createdBefore(createdBefore));

        return aiPricingRecommendationRepository
                .findAll(specification, pageable)
                .map(this::mapToManagementResponse);
    }
    private AiPricingRecommendationManagementResponse mapToManagementResponse(
            AiPricingRecommendation recommendation
    ) {
        Vehicle vehicle = recommendation.getVehicle();

        return new AiPricingRecommendationManagementResponse(
                recommendation.getId(),
                recommendation.getTenant().getId(),
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getBrand(),
                vehicle.getModel(),
                recommendation.getCurrentPrice(),
                recommendation.getRecommendedPrice(),
                recommendation.getConfidenceLevel(),
                recommendation.getReason(),
                recommendation.getStatus(),
                recommendation.getApprovedBy() != null
                        ? recommendation.getApprovedBy().getId()
                        : null,
                recommendation.getRejectedBy() != null
                        ? recommendation.getRejectedBy().getId()
                        : null,
                recommendation.getApprovedAt(),
                recommendation.getRejectedAt(),
                recommendation.getCreatedAt()
        );
    }
}
