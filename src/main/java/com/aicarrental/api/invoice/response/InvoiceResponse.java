package com.aicarrental.api.invoice.response;
import com.aicarrental.domain.invoice.InvoiceStatus;
import com.aicarrental.domain.invoice.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long tenantId,
        Long reservationId,
        Long rentalId,
        InvoiceType type,
        InvoiceStatus status,
        String customerFullNameSnapshot,
        String customerEmailSnapshot,
        String vehiclePlateNumberSnapshot,
        BigDecimal rentalAmount,
        BigDecimal extraKmAmount,
        BigDecimal depositAmount,
        BigDecimal depositDeductionAmount,
        BigDecimal refundAmount,
        BigDecimal totalAmount,
        String currency,
        LocalDateTime issuedAt
) {
}
