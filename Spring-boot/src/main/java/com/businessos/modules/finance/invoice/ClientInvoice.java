package com.businessos.modules.finance.invoice;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import com.businessos.enums.InvoiceStatus;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "client_invoices", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "invoice_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientInvoice extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber; // INV-2024-001

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    private LocalDate invoiceDate;
    private LocalDate dueDate;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ClientInvoiceItem> items = new java.util.ArrayList<>();

    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    // If set, taxAmount is (re)computed from subtotal on every calculateTotals() call.
    // Left null to keep the older "manually typed tax amount" behavior.
    private BigDecimal taxRatePercent;
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(length = 10)
    private String currency = "BDT";

    // How many units of the company's base currency one unit of `currency` is worth
    // at issue time. 1 for base-currency invoices. GL postings multiply by this so the
    // ledger stays single-currency in base - see ClientInvoiceServiceImpl#toBase.
    @Builder.Default
    @Column(precision = 15, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    // Cumulative amount written off via credit notes - reduces the balance owed
    // without reversing revenue/cash the way a full refund does.
    @Builder.Default
    private BigDecimal creditedAmount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal balanceAmount = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentTerms paymentTerms;

    private String description;
    private String notes;

    private LocalDate sentDate;
    private LocalDate paidDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;

    public void calculateTotals() {
        if (items != null) {
            subtotal = items.stream()
                    .map(ClientInvoiceItem::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        BigDecimal discount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        BigDecimal taxableBase = subtotal.subtract(discount).max(BigDecimal.ZERO);

        if (taxRatePercent != null) {
            taxAmount = taxableBase.multiply(taxRatePercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        totalAmount = taxableBase.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        BigDecimal credited = creditedAmount != null ? creditedAmount : BigDecimal.ZERO;
        balanceAmount = totalAmount.subtract(paid).subtract(credited);
    }

    public boolean isOverdue() {
        return dueDate != null && LocalDate.now().isAfter(dueDate) &&
                !InvoiceStatus.PAID.equals(status);
    }
}
