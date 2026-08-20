package com.businessos.modules.finance.payment;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.crm.client.Client;
import com.businessos.modules.finance.invoice.ClientInvoice;
import com.businessos.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.math.BigDecimal;
import java.time.LocalDate;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "payment_receipts", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "receipt_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentReceipt extends BaseEntity {

    private Long companyId;

    @Column(name = "receipt_number", nullable = false)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private ClientInvoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    private BigDecimal amount;
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentMethod paymentMethod;

    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private String notes;

    private LocalDate depositDate;
    private String depositedToBank;

    // Set when the payment bounced/was reversed (NSF cheque, chargeback...)
    private LocalDate reversedDate;
    private String reversalReason;

    public void confirmPayment() {
        this.status = PaymentStatus.CONFIRMED;
    }

    public void markAsDeposited(String bank) {
        this.status = PaymentStatus.DEPOSITED;
        this.depositDate = LocalDate.now();
        this.depositedToBank = bank;
    }
}
