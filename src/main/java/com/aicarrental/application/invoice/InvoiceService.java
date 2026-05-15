package com.aicarrental.application.invoice;
import com.aicarrental.api.invoice.response.InvoiceResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.invoice.Invoice;
import com.aicarrental.domain.invoice.InvoiceStatus;
import com.aicarrental.domain.invoice.InvoiceType;
import com.aicarrental.domain.rental.Rental;
import com.aicarrental.domain.reservation.Reservation;
import com.aicarrental.infrastructure.persistence.InvoiceRepository;
import com.aicarrental.infrastructure.persistence.RentalRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
    public Invoice createRentalCompletionInvoice(Long rentalId) {

        if (invoiceRepository.existsByRental_Id(rentalId)) {
            throw new BusinessException("Invoice already exists for this rental");
        }

        Rental rental = rentalRepository.findById(rentalId)
                .orElseThrow(() -> new ResourceNotFoundException("Rental not found"));

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

        return savedInvoice;
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
