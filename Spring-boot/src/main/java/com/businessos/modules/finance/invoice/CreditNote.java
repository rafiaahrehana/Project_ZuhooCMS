package com.businessos.modules.finance.invoice;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A partial write-down against an invoice - e.g. "we overcharged you by 500, here's
 * a credit" - without reversing the whole invoice like a refund does. Unlike Refund,
 * no cash actually leaves the company, so it's issued directly (no approve/reject
 * workflow) and simply reduces what the client still owes.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "credit_notes", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "credit_note_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditNote extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(name = "credit_note_number", nullable = false)
    private String creditNoteNumber; // CN-2026-000001

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_invoice_id", nullable = false)
    private ClientInvoice clientInvoice;

    @Column(nullable = false)
    private BigDecimal amount;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_user_id")
    private User issuedBy;

    private LocalDateTime issuedAt;
}
