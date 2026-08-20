package com.businessos.modules.finance.invoice;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "client_invoice_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientInvoiceItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private ClientInvoice invoice;

    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    private String notes;

    public void calculateLineTotal() {
        lineTotal = quantity.multiply(unitPrice);
    }
}
