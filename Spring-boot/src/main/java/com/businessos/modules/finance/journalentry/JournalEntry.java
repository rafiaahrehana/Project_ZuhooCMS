package com.businessos.modules.finance.journalentry;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
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
@Table(name = "journal_entries", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "journal_entry_number"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntry extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(name = "journal_entry_number", nullable = false)
    private String journalEntryNumber; // JE-2024-001

    private LocalDate entryDate;

    // Legacy 1:1 columns. The lines collection below is now authoritative; these stay
    // populated (first debit line's account / first credit line's account / total amount)
    // because the existing DB columns are NOT NULL and ddl-auto=update never drops
    // constraints - and they keep old pre-lines entries displayable.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "debit_account_id", nullable = false)
    private ChartOfAccount debitAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_account_id", nullable = false)
    private ChartOfAccount creditAccount;

    private BigDecimal amount; // total debits (= total credits)

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private java.util.List<JournalEntryLine> lines = new java.util.ArrayList<>();

    private String description;
    private String notes;

    private String createdBy; // User who created
    private LocalDate createdDate;

    private String approvedBy; // User who approved
    private LocalDate approvedDate;

    @Builder.Default
    private boolean approved = false;

    @Builder.Default
    private boolean posted = false;

    private LocalDate postedDate;

    // ── Reversal ──────────────────────────────────────────────
    // A posted entry can be reversed once. The reversal is a second entry with
    // debit/credit swapped; these fields link the two together.
    @Builder.Default
    private boolean reversed = false;

    private Long reversalEntryId;      // on the original: the entry that reverses it
    private Long reversedFromEntryId;  // on the reversal: the original it reverses
    private LocalDate reversedDate;

    public void markReversed(Long reversalEntryId) {
        this.reversed = true;
        this.reversalEntryId = reversalEntryId;
        this.reversedDate = LocalDate.now();
    }

    public void approve(String approverName) {
        this.approved = true;
        this.approvedBy = approverName;
        this.approvedDate = LocalDate.now();
    }

    public void post() {
        if (!approved) {
            throw new IllegalStateException("Journal entry must be approved before posting");
        }
        this.posted = true;
        this.postedDate = LocalDate.now();
    }
}
