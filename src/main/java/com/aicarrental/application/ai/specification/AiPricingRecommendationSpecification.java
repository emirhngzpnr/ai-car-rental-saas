package com.aicarrental.application.ai.specification;

import com.aicarrental.domain.ai.AiPricingRecommendation;
import com.aicarrental.domain.ai.AiPricingRecommendationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class AiPricingRecommendationSpecification {
    public static Specification<AiPricingRecommendation> hasTenantId(
            Long tenantId
    ) {
        return (root, query, criteriaBuilder) ->
                tenantId == null
                        ? null
                        : criteriaBuilder.equal(
                        root.get("tenant").get("id"),
                        tenantId
                );
    }

    public static Specification<AiPricingRecommendation> hasStatus(
            AiPricingRecommendationStatus status
    ) {
        return (root, query, criteriaBuilder) ->
                status == null
                        ? null
                        : criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<AiPricingRecommendation> hasVehicleId(
            Long vehicleId
    ) {
        return (root, query, criteriaBuilder) ->
                vehicleId == null
                        ? null
                        : criteriaBuilder.equal(
                        root.get("vehicle").get("id"),
                        vehicleId
                );
    }

    public static Specification<AiPricingRecommendation> createdAfter(
            LocalDateTime createdAfter
    ) {
        return (root, query, criteriaBuilder) ->
                createdAfter == null
                        ? null
                        : criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        createdAfter
                );
    }

    public static Specification<AiPricingRecommendation> createdBefore(
            LocalDateTime createdBefore
    ) {
        return (root, query, criteriaBuilder) ->
                createdBefore == null
                        ? null
                        : criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdAt"),
                        createdBefore
                );
    }
}
