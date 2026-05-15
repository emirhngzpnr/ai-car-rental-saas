package com.aicarrental.domain.invoice;
import com.aicarrental.domain.tenant.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "invoice_counters",
        schema = "rental",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_invoice_counters_tenant_year",
                        columnNames = {"tenant_id", "invoice_year"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCounter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "invoice_year", nullable = false)
    private Integer invoiceYear;

    @Column(name = "last_number", nullable = false)
    private Long lastNumber;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();

        if (this.lastNumber == null) {
            this.lastNumber = 0L;
        }
    }
}
