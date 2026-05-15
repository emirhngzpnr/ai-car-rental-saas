package com.aicarrental.api.invoice;
import com.aicarrental.api.invoice.response.InvoiceResponse;
import com.aicarrental.application.invoice.InvoicePdfService;
import com.aicarrental.application.invoice.InvoiceService;
import com.aicarrental.domain.invoice.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @PostMapping("/rental/{rentalId}")
    public ResponseEntity<InvoiceResponse> createRentalCompletionInvoice(
            @PathVariable Long rentalId
    ) {
        Invoice invoice = invoiceService.createRentalCompletionInvoice(rentalId);
        return ResponseEntity.ok(invoiceService.mapToResponse(invoice));
    }
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable Long id
    ) {
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(id);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=invoice-" + id + ".pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}
