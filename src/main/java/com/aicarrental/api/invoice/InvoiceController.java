package com.aicarrental.api.invoice;
import com.aicarrental.api.invoice.response.InvoiceResponse;
import com.aicarrental.application.invoice.InvoicePdfService;
import com.aicarrental.application.invoice.InvoiceService;
import com.aicarrental.domain.invoice.Invoice;
import com.aicarrental.domain.invoice.InvoiceStatus;
import com.aicarrental.domain.invoice.InvoiceType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        return ResponseEntity.ok(
                invoiceService.createRentalCompletionInvoiceForCurrentUser(rentalId)
        );
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) InvoiceType type,
            Pageable pageable
    ) {
        return ResponseEntity.ok(invoiceService.getInvoices(status, type, pageable));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @PathVariable Long id
    ) {
        Invoice invoice = invoiceService.getAccessibleInvoice(id);
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoice);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + invoice.getInvoiceNumber() + ".pdf"
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}
