package com.aicarrental.application.invoice;

import com.aicarrental.domain.invoice.Invoice;
import com.aicarrental.infrastructure.notification.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceEmailService {
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final InvoicePdfService invoicePdfService;
    private final EmailSender emailSender;

    public void sendRentalCompletionInvoice(Invoice invoice) {
        byte[] pdf = invoicePdfService.generateInvoicePdf(invoice);
        String invoiceNumber = invoice.getInvoiceNumber();
        String recipient = invoice.getCustomerEmailSnapshot();

        emailSender.sendEmailWithAttachment(
                recipient,
                "Your AI Car Rental invoice " + invoiceNumber,
                """
                        Your rental has been completed.
                        
                        Please find your invoice attached as a PDF.
                        
                        Invoice number: %s
                        """.formatted(invoiceNumber),
                invoiceNumber + ".pdf",
                pdf,
                PDF_CONTENT_TYPE
        );
    }
}
