package com.aicarrental.application.customer;

import com.aicarrental.api.customer.request.CustomerCreateReservationRequest;
import com.aicarrental.api.customer.response.CustomerReservationResponse;
import com.aicarrental.api.publicapi.request.PublicCreateReservationRequest;
import com.aicarrental.api.publicapi.response.PublicDepositPaymentResponse;
import com.aicarrental.api.publicapi.response.PublicReservationResponse;
import com.aicarrental.application.publicapi.PublicPaymentService;
import com.aicarrental.application.publicapi.PublicReservationService;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentCustomerService;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.domain.payment.PaymentStatus;
import com.aicarrental.domain.payment.PaymentType;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.infrastructure.persistence.PaymentTransactionRepository;
import com.aicarrental.infrastructure.persistence.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerReservationService {
    private final CurrentCustomerService currentCustomerService;
    private final PublicReservationService publicReservationService;
    private final PublicPaymentService publicPaymentService;
    private final ReservationRepository reservationRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional
    public PublicReservationResponse create(CustomerCreateReservationRequest request) {
        CustomerAccount customer = currentCustomerService.getCurrentCustomer();
        PublicCreateReservationRequest publicRequest = new PublicCreateReservationRequest(
                request.vehicleId(),
                customer.getFirstName() + " " + customer.getLastName(),
                customer.getPhone(),
                customer.getEmail(),
                request.customerIdentityNumber(),
                request.pickupDateTime(),
                request.returnDateTime(),
                request.insurancePackageId()
        );
        return publicReservationService.createReservationForCustomer(
                request.tenantSlug(), publicRequest, customer
        );
    }

    @Transactional(readOnly = true)
    public List<CustomerReservationResponse> getReservations() {
        CustomerAccount customer = currentCustomerService.getCurrentCustomer();
        return reservationRepository.findByCustomerAccount_IdAndActiveTrueOrderByCreatedAtDesc(customer.getId())
                .stream().map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public CustomerReservationResponse getReservation(String reservationCode) {
        CustomerAccount customer = currentCustomerService.getCurrentCustomer();
        Reservation reservation = reservationRepository
                .findByReservationCodeAndCustomerAccount_IdAndActiveTrue(reservationCode, customer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        return map(reservation);
    }

    @Transactional
    public PublicDepositPaymentResponse payDeposit(String reservationCode, String idempotencyKey) {
        return publicPaymentService.payDepositForCustomer(
                currentCustomerService.getCurrentCustomer(), reservationCode, idempotencyKey
        );
    }

    private CustomerReservationResponse map(Reservation reservation) {
        boolean paid = paymentTransactionRepository.existsByReservation_IdAndPaymentTypeAndPaymentStatus(
                reservation.getId(), PaymentType.DEPOSIT_PAYMENT, PaymentStatus.SUCCESS
        );
        return new CustomerReservationResponse(
                reservation.getReservationCode(),
                reservation.getStatus().name(),
                reservation.getTenant().getSlug(),
                reservation.getTenant().getCompanyName(),
                reservation.getVehicle().getId(),
                reservation.getVehicle().getBrand(),
                reservation.getVehicle().getModel(),
                reservation.getPickupDateTime(),
                reservation.getReturnDateTime(),
                reservation.getDepositAmount(),
                reservation.getEstimatedRentalPrice(),
                reservation.getInsuranceTotalPriceSnapshot(),
                reservation.getTotalEstimatedPrice(),
                paid ? "DEPOSIT_PAID" : "DEPOSIT_PENDING"
        );
    }
}
