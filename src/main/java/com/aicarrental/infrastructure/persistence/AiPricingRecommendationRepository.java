package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.ai.AiPricingRecommendation;
import com.aicarrental.domain.ai.AiPricingRecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiPricingRecommendationRepository extends JpaRepository<AiPricingRecommendation, Long> {
    List<AiPricingRecommendation> findByTenant_IdAndStatusOrderByCreatedAtDesc(
            Long tenantId,
            AiPricingRecommendationStatus status
    );

    List<AiPricingRecommendation> findByStatusOrderByCreatedAtDesc(
            AiPricingRecommendationStatus status
    );

}
