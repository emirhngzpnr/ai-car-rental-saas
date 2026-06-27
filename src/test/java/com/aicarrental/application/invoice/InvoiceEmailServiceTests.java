package com.aicarrental.application.invoice;

import com.aicarrental.domain.invoice.Invoice;
import com.aicarrental.infrastructure.notification.EmailSender;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvoiceEmailServiceTests {
    @Test
    void sendsRentalCompletionInvoiceAsPdfAttachment() {
        InvoicePdfService pdfService = mock(InvoicePdfService.class);
        EmailSender emailSender = mock(EmailSender.class);
        InvoiceEmailService service = new InvoiceEmailService(pdfService, emailSender);
        Invoice invoice = Invoice.builder()
                .invoiceNumber("INV-2-2026-000007")
                .customerEmailSnapshot("customer@example.com")
                .build();
        byte[] pdf = new byte[]{1, 2, 3};
        when(pdfService.generateInvoicePdf(invoice)).thenReturn(pdf);

        service.sendRentalCompletionInvoice(invoice);

        verify(emailSender).sendEmailWithAttachment(
                eq("customer@example.com"),
                eq("Your AI Car Rental invoice INV-2-2026-000007"),
                org.mockito.ArgumentMatchers.contains("Invoice number: INV-2-2026-000007"),
                eq("INV-2-2026-000007.pdf"),
                eq(pdf),
                eq("application/pdf")
        );
    }
}
