package com.aicarrental.application.customer;

import com.aicarrental.api.customer.request.CustomerVehicleReviewRequest;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.security.CurrentCustomerService;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.domain.rental.Rental;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.review.VehicleReview;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.infrastructure.persistence.RentalRepository;
import com.aicarrental.infrastructure.persistence.VehicleReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerVehicleReviewServiceTests {
    @Mock CurrentCustomerService currentCustomerService;
    @Mock RentalRepository rentalRepository;
    @Mock VehicleReviewRepository reviewRepository;

    private CustomerVehicleReviewService service;
    private CustomerAccount customer;
    private Reservation reservation;
    private Rental rental;

    @BeforeEach
    void setUp() {
        service = new CustomerVehicleReviewService(
                currentCustomerService,
                rentalRepository,
                reviewRepository
        );

        customer = CustomerAccount.builder()
                .id(11L)
                .firstName("Emirhan")
                .lastName("Efe")
                .email("customer@example.com")
                .build();
        Tenant tenant = Tenant.builder().id(3L).companyName("CityDrive").build();
        Vehicle vehicle = Vehicle.builder().id(7L).brand("Renault").model("Clio").tenant(tenant).build();
        reservation = Reservation.builder()
                .id(19L)
                .reservationCode("RNT-2026-000019")
                .customerAccount(customer)
                .tenant(tenant)
                .vehicle(vehicle)
                .build();
        rental = Rental.builder()
                .id(23L)
                .tenant(tenant)
                .vehicle(vehicle)
                .reservation(reservation)
                .build();
    }

    @Test
    void createAllowsCompletedRentalOwnerToReviewOnce() {
        when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);
        when(rentalRepository.findCompletedCustomerRentalForReview("RNT-2026-000019", 11L))
                .thenReturn(Optional.of(rental));
        when(reviewRepository.existsByReservation_Id(19L)).thenReturn(false);
        when(reviewRepository.save(any(VehicleReview.class))).thenAnswer(invocation -> {
            VehicleReview review = invocation.getArgument(0);
            review.setId(31L);
            return review;
        });

        var response = service.create(
                "RNT-2026-000019",
                new CustomerVehicleReviewRequest(5, "Great car", "Clean and reliable.")
        );

        assertEquals(31L, response.id());
        assertEquals(5, response.rating());
        assertEquals("RNT-2026-000019", response.reservationCode());
        verify(reviewRepository).save(any(VehicleReview.class));
    }

    @Test
    void createRejectsDuplicateReviewForSameReservation() {
        when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);
        when(rentalRepository.findCompletedCustomerRentalForReview("RNT-2026-000019", 11L))
                .thenReturn(Optional.of(rental));
        when(reviewRepository.existsByReservation_Id(19L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.create(
                "RNT-2026-000019",
                new CustomerVehicleReviewRequest(4, null, "Already reviewed.")
        ));
    }

    @Test
    void createRejectsReservationWithoutCompletedRentalOwnership() {
        when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);
        when(rentalRepository.findCompletedCustomerRentalForReview("RNT-2026-000019", 11L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.create(
                "RNT-2026-000019",
                new CustomerVehicleReviewRequest(4, null, "Not eligible.")
        ));
    }

    @Test
    void updateRejectsReviewsAfterEditWindowExpires() {
        VehicleReview review = VehicleReview.builder()
                .id(31L)
                .reservation(reservation)
                .vehicle(rental.getVehicle())
                .customerAccount(customer)
                .rating(5)
                .title("Original")
                .comment("Original comment")
                .active(true)
                .createdAt(LocalDateTime.now().minusMinutes(11))
                .build();

        when(currentCustomerService.getCurrentCustomer()).thenReturn(customer);
        when(rentalRepository.findCompletedCustomerRentalForReview("RNT-2026-000019", 11L))
                .thenReturn(Optional.of(rental));
        when(reviewRepository.findByReservation_IdAndCustomerAccount_IdAndActiveTrue(19L, 11L))
                .thenReturn(Optional.of(review));

        assertThrows(BusinessException.class, () -> service.update(
                "RNT-2026-000019",
                new CustomerVehicleReviewRequest(4, "Updated", "Updated comment")
        ));
    }
}
