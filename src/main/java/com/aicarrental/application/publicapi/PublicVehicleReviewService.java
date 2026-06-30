package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.response.PublicVehicleReviewPageResponse;
import com.aicarrental.api.publicapi.response.PublicVehicleReviewResponse;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.domain.review.VehicleReview;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import com.aicarrental.infrastructure.persistence.VehicleReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicVehicleReviewService {
    private final VehicleRepository vehicleRepository;
    private final VehicleReviewRepository reviewRepository;

    public PublicVehicleReviewPageResponse getReviews(Long vehicleId, int page, int size) {
        if (vehicleRepository.findByIdAndActiveTrueAndTenant_ActiveTrue(vehicleId).isEmpty()) {
            throw new ResourceNotFoundException("Vehicle not found");
        }

        int safePage = Math.max(0, page);
        int safeSize = Math.min(20, Math.max(1, size));
        Page<VehicleReview> result = reviewRepository.findPublicReviewsByVehicleId(
                vehicleId,
                PageRequest.of(safePage, safeSize)
        );

        return new PublicVehicleReviewPageResponse(
                result.getContent().stream().map(this::map).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private PublicVehicleReviewResponse map(VehicleReview review) {
        return new PublicVehicleReviewResponse(
                review.getId(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                displayName(review.getCustomerAccount()),
                review.getCreatedAt()
        );
    }

    private String displayName(CustomerAccount customer) {
        String firstName = customer.getFirstName() == null ? "" : customer.getFirstName().trim();
        String lastName = customer.getLastName() == null ? "" : customer.getLastName().trim();
        if (firstName.isBlank()) {
            return "Customer";
        }
        if (lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName.charAt(0) + ".";
    }
}
