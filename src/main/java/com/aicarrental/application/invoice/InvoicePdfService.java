package com.aicarrental.application.invoice;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.invoice.Invoice;
import com.aicarrental.infrastructure.persistence.InvoiceRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {
    private final InvoiceRepository invoiceRepository;
    private final TemplateEngine templateEngine;

    public byte[] generateInvoicePdf(Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invoice not found")
                );

        try {

            Context context = new Context();
            context.setVariable("invoice", invoice);

            String html =
                    templateEngine.process(
                            "invoice/invoice-template",
                            context
                    );

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            PdfRendererBuilder builder =
                    new PdfRendererBuilder();

            builder.useFastMode();

            builder.withHtmlContent(
                    html,
                    null
            );

            builder.toStream(outputStream);

            builder.run();

            return outputStream.toByteArray();

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to generate invoice PDF",
                    exception
            );
        }
    }
}
