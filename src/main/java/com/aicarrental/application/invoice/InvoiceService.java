package com.aicarrental.application.invoice;
import com.aicarrental.api.invoice.response.InvoiceResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.invoice.Invoice;
import com.aicarrental.domain.invoice.InvoiceStatus;
import com.aicarrental.domain.invoice.InvoiceType;
import com.aicarrental.domain.rental.Rental;
import com.aicarrental.domain.rental.RentalStatus;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.infrastructure.persistence.InvoiceRepository;
import com.aicarrental.infrastructure.persistence.RentalRepository;
import com.aicarrental.application.report.ReportCacheInvalidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final RentalRepository rentalRepository;
    private final InvoiceNumberGeneratorService invoiceNumberGeneratorService;
    private final AuditEventPublisher auditEventPublisher;
    private final ReportCacheInvalidator reportCacheInvalidator;
    private final CurrentUserService currentUserService;

    public Invoice createRentalCompletionInvoiceIfAbsent(Long rentalId) {
        return invoiceRepository.findByRental_Id(rentalId)
                .orElseGet(() -> createRentalCompletionInvoice(rentalId));
    }

    public InvoiceResponse createRentalCompletionInvoiceForCurrentUser(Long rentalId) {
        Invoice existingInvoice = findExistingInvoiceForCurrentUser(rentalId);
        if (existingInvoice != null) {
            return mapToResponse(existingInvoice);
        }

        Rental rental = findRentalForCurrentUser(rentalId);
        return mapToResponse(createRentalCompletionInvoice(rental));
    }

    public Invoice createRentalCompletionInvoice(Long rentalId) {
        if (invoiceRepository.existsByRental_Id(rentalId)) {
            throw new BusinessException("Invoice already exists for this rental");
        }

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));

        return createRentalCompletionInvoice(rental);
    }

    public Page<InvoiceResponse> getInvoices(
            InvoiceStatus status,
            InvoiceType type,
            Pageable pageable
    ) {
        Long tenantId = currentUserService.isSuperAdmin()
                ? null
                : currentUserService.getCurrentTenantId();

        return invoiceRepository.findInvoices(tenantId, status, type, pageable)
                .map(this::mapToResponse);
    }

    public Invoice getAccessibleInvoice(Long invoiceId) {
        if (currentUserService.isSuperAdmin()) {
            return invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        }

        return invoiceRepository.findByIdAndTenant_Id(
                        invoiceId,
                        currentUserService.getCurrentTenantId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    private Invoice createRentalCompletionInvoice(Rental rental) {
        if (rental.getStatus() != RentalStatus.COMPLETED) {
            throw new BusinessException("Invoice can only be issued for completed rentals");
        }

        Reservation reservation = rental.getReservation();

        if (reservation == null) {
            throw new BusinessException("Rental is not linked to a reservation");
        }

        Long tenantId = rental.getTenant().getId();

        String invoiceNumber =
                invoiceNumberGeneratorService.generateInvoiceNumber(tenantId);

        BigDecimal rentalAmount =
                rental.getFinalRentalPrice() != null
                        ? rental.getFinalRentalPrice()
                        : reservation.getEstimatedRentalPrice();

        BigDecimal extraKmAmount =
                rental.getExtraKmFee() != null
                        ? rental.getExtraKmFee()
                        : BigDecimal.ZERO;

        BigDecimal depositAmount =
                reservation.getDepositAmount() != null
                        ? reservation.getDepositAmount()
                        : BigDecimal.ZERO;

        BigDecimal depositDeductionAmount =
                rental.getDepositDeduction() != null
                        ? rental.getDepositDeduction()
                        : BigDecimal.ZERO;

        BigDecimal refundAmount =
                rental.getRefundAmount() != null
                        ? rental.getRefundAmount()
                        : depositAmount.subtract(depositDeductionAmount);

        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }

        BigDecimal totalAmount =
                rentalAmount.add(extraKmAmount).subtract(refundAmount);

        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .tenant(rental.getTenant())
                .reservation(reservation)
                .rental(rental)

                .type(InvoiceType.RENTAL_COMPLETION)
                .status(InvoiceStatus.ISSUED)

                .customerFullNameSnapshot(reservation.getCustomerFullName())
                .customerEmailSnapshot(reservation.getCustomerEmail())
                .customerPhoneSnapshot(reservation.getCustomerPhone())
                .customerIdentityNumberSnapshot(reservation.getCustomerIdentityNumber())

                .vehiclePlateNumberSnapshot(
                        reservation.getVehicle() != null
                                ? reservation.getVehicle().getPlateNumber()
                                : "UNKNOWN"
                )
                .vehicleBrandSnapshot(
                        reservation.getVehicle() != null
                                ? reservation.getVehicle().getBrand()
                                : null
                )
                .vehicleModelSnapshot(
                        reservation.getVehicle() != null
                                ? reservation.getVehicle().getModel()
                                : null
                )

                .rentalAmount(rentalAmount)
                .extraKmAmount(extraKmAmount)
                .depositAmount(depositAmount)
                .depositDeductionAmount(depositDeductionAmount)
                .refundAmount(refundAmount)
                .totalAmount(totalAmount)
                .currency("TRY")
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        auditEventPublisher.publish(new AuditEvent(
                null,
                "SYSTEM",
                "SYSTEM",
                savedInvoice.getTenant() != null ? savedInvoice.getTenant().getId() : null,
                AuditAction.INVOICE_CREATED,
                "Invoice",
                savedInvoice.getId(),
                "Invoice created. Invoice number: " + savedInvoice.getInvoiceNumber()
        ));
        reportCacheInvalidator.evictAfterCommit();

        return savedInvoice;
    }

    private Invoice findExistingInvoiceForCurrentUser(Long rentalId) {
        if (currentUserService.isSuperAdmin()) {
            return invoiceRepository.findByRental_Id(rentalId).orElse(null);
        }

        return invoiceRepository.findByRental_IdAndTenant_Id(
                rentalId,
                currentUserService.getCurrentTenantId()
        ).orElse(null);
    }

    private Rental findRentalForCurrentUser(Long rentalId) {
        if (currentUserService.isSuperAdmin()) {
            return rentalRepository.findById(rentalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));
        }

        return rentalRepository.findByIdAndTenant_IdAndActiveTrue(
                        rentalId,
                        currentUserService.getCurrentTenantId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));
    }

    public InvoiceResponse mapToResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getTenant() != null ? invoice.getTenant().getId() : null,
                invoice.getReservation() != null ? invoice.getReservation().getId() : null,
                invoice.getRental() != null ? invoice.getRental().getId() : null,
                invoice.getType(),
                invoice.getStatus(),
                invoice.getCustomerFullNameSnapshot(),
                invoice.getCustomerEmailSnapshot(),
                invoice.getVehiclePlateNumberSnapshot(),
                invoice.getRentalAmount(),
                invoice.getExtraKmAmount(),
                invoice.getDepositAmount(),
                invoice.getDepositDeductionAmount(),
                invoice.getRefundAmount(),
                invoice.getTotalAmount(),
                invoice.getCurrency(),
                invoice.getIssuedAt()
        );
    }
}
