package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.request.PublicCreateReservationRequest;
import com.aicarrental.api.publicapi.response.PublicReservationResponse;
import com.aicarrental.api.publicapi.response.PublicReservationTrackingResponse;
import com.aicarrental.application.outbox.OutboxMessageService;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.event.ReservationCreatedEvent;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.insurance.InsurancePackage;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.domain.outbox.OutboxEventType;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.domain.reservation.ReservationStatus;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.domain.vehicle.Vehicle;
import com.aicarrental.domain.vehicle.VehicleStatus;
import com.aicarrental.infrastructure.persistence.InsurancePackageRepository;
import com.aicarrental.infrastructure.persistence.PaymentTransactionRepository;
import com.aicarrental.infrastructure.persistence.ReservationRepository;
import com.aicarrental.infrastructure.persistence.VehicleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicReservationService {
    private final PublicTenantService publicTenantService;
    private final VehicleRepository vehicleRepository;
    private final ReservationRepository reservationRepository;
    private final InsurancePackageRepository insurancePackageRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OutboxMessageService outboxMessageService;
    private final AuditEventPublisher auditEventPublisher;

    public PublicReservationResponse createReservation(String tenantSlug, PublicCreateReservationRequest request) {
        return createReservationInternal(tenantSlug, request, null);
    }

    public PublicReservationResponse createReservationForCustomer(
            String tenantSlug,
            PublicCreateReservationRequest request,
            CustomerAccount customerAccount
    ) {
        return createReservationInternal(tenantSlug, request, customerAccount);
    }

    private PublicReservationResponse createReservationInternal(
            String tenantSlug,
            PublicCreateReservationRequest request,
            CustomerAccount customerAccount
    ) {
        PublicVehicleService.validateDateRange(request.pickupDateTime(), request.returnDateTime());
        Tenant tenant = publicTenantService.findActiveTenant(tenantSlug);

        Vehicle vehicle = vehicleRepository.findByIdAndTenantIdForUpdate(request.vehicleId(), tenant.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        if (!Boolean.TRUE.equals(vehicle.getActive()) || vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new BusinessException("Vehicle is not available");
        }

        if (reservationRepository.existsOverlappingReservation(
                vehicle.getId(),
                request.pickupDateTime(),
                request.returnDateTime()
        )) {
            throw new BusinessException("Vehicle is already reserved for selected dates");
        }

        long rentalDays = Math.max(1, Duration.between(request.pickupDateTime(), request.returnDateTime()).toDays());
        BigDecimal estimatedRentalPrice = vehicle.getDailyPrice().multiply(BigDecimal.valueOf(rentalDays));

        InsurancePackage insurancePackage = null;
        BigDecimal insuranceTotalPrice = BigDecimal.ZERO;

        if (request.insurancePackageId() != null) {
            insurancePackage = insurancePackageRepository
                    .findByIdAndTenant_IdAndActiveTrue(request.insurancePackageId(), tenant.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insurance package not found"));

            insuranceTotalPrice = insurancePackage.getDailyPrice()
                    .multiply(BigDecimal.valueOf(rentalDays))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal depositAmount = estimatedRentalPrice
                .multiply(BigDecimal.valueOf(0.30))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalEstimatedPrice = estimatedRentalPrice.add(depositAmount).add(insuranceTotalPrice);
        LocalDateTime now = LocalDateTime.now();

        Reservation reservation = Reservation.builder()
                .tenant(tenant)
                .vehicle(vehicle)
                .customerFullName(request.customerFullName())
                .customerPhone(request.customerPhone().trim())
                .customerEmail(request.customerEmail())
                .customerIdentityNumber(request.customerIdentityNumber())
                .pickupDateTime(request.pickupDateTime())
                .returnDateTime(request.returnDateTime())
                .dailyPriceSnapshot(vehicle.getDailyPrice())
                .dailyKmLimitSnapshot(vehicle.getDailyKmLimit())
                .extraKmPricePerKmSnapshot(vehicle.getExtraKmPricePerKm())
                .insurancePackage(insurancePackage)
                .customerAccount(customerAccount)
                .insurancePackageNameSnapshot(insurancePackage != null ? insurancePackage.getName() : null)
                .insurancePackageTypeSnapshot(insurancePackage != null ? insurancePackage.getType().name() : null)
                .insuranceDailyPriceSnapshot(insurancePackage != null ? insurancePackage.getDailyPrice() : null)
                .insuranceTotalPriceSnapshot(insurancePackage != null ? insuranceTotalPrice : null)
                .depositAmount(depositAmount)
                .estimatedRentalPrice(estimatedRentalPrice)
                .totalEstimatedPrice(totalEstimatedPrice)
                .status(ReservationStatus.PENDING_PAYMENT)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        savedReservation.setReservationCode(generateReservationCode(savedReservation));
        savedReservation = reservationRepository.save(savedReservation);

        publishReservationCreated(savedReservation);
        publishAudit(savedReservation, AuditAction.RESERVATION_CREATED, "Public reservation created");

        return mapReservation(savedReservation);
    }

    public PublicReservationTrackingResponse trackReservation(String reservationCode, String email) {
        Reservation reservation = reservationRepository
                .findByReservationCodeAndCustomerEmailIgnoreCaseAndActiveTrue(reservationCode, email)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        boolean depositPaid = paymentTransactionRepository.existsByReservation_IdAndPaymentTypeAndPaymentStatus(
                reservation.getId(),
                com.aicarrental.domain.payment.PaymentType.DEPOSIT_PAYMENT,
                com.aicarrental.domain.payment.PaymentStatus.SUCCESS
        );

        return new PublicReservationTrackingResponse(
                reservation.getReservationCode(),
                reservation.getStatus().name(),
                reservation.getVehicle().getBrand(),
                reservation.getVehicle().getModel(),
                PublicVehicleService.maskPlateNumber(reservation.getVehicle().getPlateNumber()),
                reservation.getPickupDateTime(),
                reservation.getReturnDateTime(),
                reservation.getDepositAmount(),
                reservation.getTotalEstimatedPrice(),
                depositPaid ? "DEPOSIT_PAID" : "DEPOSIT_PENDING"
        );
    }

    Reservation findPublicReservationForPayment(String tenantSlug, String reservationCode, String email) {
        Tenant tenant = publicTenantService.findActiveTenant(tenantSlug);
        return reservationRepository
                .findByReservationCodeAndTenant_IdAndCustomerEmailIgnoreCaseAndActiveTrue(
                        reservationCode,
                        tenant.getId(),
                        email
                )
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    Reservation findPublicReservationForPaymentForUpdate(String tenantSlug, String reservationCode, String email) {
        Tenant tenant = publicTenantService.findActiveTenant(tenantSlug);
        return reservationRepository
                .findPublicReservationForPaymentForUpdate(
                        reservationCode,
                        tenant.getId(),
                        email
                )
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
    }

    PublicReservationResponse mapReservation(Reservation reservation) {
        return new PublicReservationResponse(
                reservation.getReservationCode(),
                reservation.getStatus().name(),
                reservation.getCustomerEmail(),
                reservation.getVehicle().getBrand(),
                reservation.getVehicle().getModel(),
                PublicVehicleService.maskPlateNumber(reservation.getVehicle().getPlateNumber()),
                reservation.getPickupDateTime(),
                reservation.getReturnDateTime(),
                reservation.getDepositAmount(),
                reservation.getEstimatedRentalPrice(),
                reservation.getInsuranceTotalPriceSnapshot(),
                reservation.getTotalEstimatedPrice()
        );
    }

    private void publishReservationCreated(Reservation reservation) {
        outboxMessageService.createOutboxMessage(
                "reservation-created",
                reservation.getId().toString(),
                OutboxEventType.RESERVATION_CREATED,
                new ReservationCreatedEvent(
                        reservation.getId(),
                        reservation.getTenant().getId(),
                        reservation.getVehicle().getId(),
                        reservation.getCustomerFullName(),
                        reservation.getCustomerEmail(),
                        reservation.getVehicle().getPlateNumber(),
                        reservation.getVehicle().getBrand(),
                        reservation.getVehicle().getModel(),
                        reservation.getPickupDateTime(),
                        reservation.getReturnDateTime(),
                        reservation.getTotalEstimatedPrice(),
                        LocalDateTime.now()
                )
        );
    }

    private void publishAudit(Reservation reservation, AuditAction action, String description) {
        auditEventPublisher.publish(new AuditEvent(
                null,
                reservation.getCustomerEmail(),
                "PUBLIC_CUSTOMER",
                reservation.getTenant().getId(),
                action,
                "Reservation",
                reservation.getId(),
                description + ": " + reservation.getReservationCode()
        ));
    }

    private String generateReservationCode(Reservation reservation) {
        int year = reservation.getCreatedAt() != null
                ? reservation.getCreatedAt().getYear()
                : LocalDateTime.now().getYear();
        return "RNT-" + year + "-" + String.format("%06d", reservation.getId());
    }
}
