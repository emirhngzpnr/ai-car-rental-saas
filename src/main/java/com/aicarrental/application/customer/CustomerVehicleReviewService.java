package com.aicarrental.application.customer;

import com.aicarrental.api.customer.request.CustomerVehicleReviewRequest;
import com.aicarrental.api.customer.response.CustomerVehicleReviewResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentCustomerService;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.domain.rental.Rental;
import com.aicarrental.domain.review.VehicleReview;
import com.aicarrental.infrastructure.persistence.RentalRepository;
import com.aicarrental.infrastructure.persistence.VehicleReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerVehicleReviewService {
    private static final int REVIEW_EDIT_WINDOW_MINUTES = 10;

    private final CurrentCustomerService currentCustomerService;
    private final RentalRepository rentalRepository;
    private final VehicleReviewRepository reviewRepository;

    @Transactional
    public CustomerVehicleReviewResponse create(String reservationCode, CustomerVehicleReviewRequest request) {
        CustomerAccount customer = currentCustomerService.getCurrentCustomer();
        Rental rental = findEligibleRental(reservationCode, customer.getId());

        if (reviewRepository.existsByReservation_Id(rental.getReservation().getId())) {
            throw new BusinessException("This reservation already has a review");
        }

        LocalDateTime now = LocalDateTime.now();
        VehicleReview review = VehicleReview.builder()
                .tenant(rental.getTenant())
                .vehicle(rental.getVehicle())
                .customerAccount(customer)
                .reservation(rental.getReservation())
                .rating(request.rating())
                .title(cleanOptional(request.title()))
                .comment(request.comment().trim())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        return map(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public CustomerVehicleReviewResponse getOwnReview(String reservationCode) {
        CustomerAccount customer = currentCustomerService.getCurrentCustomer();
        Rental rental = findEligibleRental(reservationCode, customer.getId());
        VehicleReview review = reviewRepository
                .findByReservation_IdAndCustomerAccount_IdAndActiveTrue(
                        rental.getReservation().getId(),
                        customer.getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        return map(review);
    }

    @Transactional
    public CustomerVehicleReviewResponse update(String reservationCode, CustomerVehicleReviewRequest request) {
        CustomerAccount customer = currentCustomerService.getCurrentCustomer();
        Rental rental = findEligibleRental(reservationCode, customer.getId());
        VehicleReview review = reviewRepository
                .findByReservation_IdAndCustomerAccount_IdAndActiveTrue(
                        rental.getReservation().getId(),
                        customer.getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        ensureReviewCanStillBeEdited(review);
        review.setRating(request.rating());
        review.setTitle(cleanOptional(request.title()));
        review.setComment(request.comment().trim());
        review.setUpdatedAt(LocalDateTime.now());

        return map(review);
    }

    @Transactional
    public void delete(String reservationCode) {
        CustomerAccount customer = currentCustomerService.getCurrentCustomer();
        Rental rental = findEligibleRental(reservationCode, customer.getId());
        VehicleReview review = reviewRepository
                .findByReservation_IdAndCustomerAccount_IdAndActiveTrue(
                        rental.getReservation().getId(),
                        customer.getId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        review.setActive(false);
        review.setUpdatedAt(LocalDateTime.now());
    }

    private Rental findEligibleRental(String reservationCode, Long customerId) {
        return rentalRepository
                .findCompletedCustomerRentalForReview(reservationCode, customerId)
                .orElseThrow(() -> new BusinessException("Only completed rentals can be reviewed"));
    }

    private void ensureReviewCanStillBeEdited(VehicleReview review) {
        if (review.getCreatedAt() == null
                || review.getCreatedAt().plusMinutes(REVIEW_EDIT_WINDOW_MINUTES).isBefore(LocalDateTime.now())) {
            throw new BusinessException("Reviews can only be edited within the first 10 minutes");
        }
    }

    private CustomerVehicleReviewResponse map(VehicleReview review) {
        return new CustomerVehicleReviewResponse(
                review.getId(),
                review.getReservation().getReservationCode(),
                review.getVehicle().getId(),
                review.getVehicle().getBrand(),
                review.getVehicle().getModel(),
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
