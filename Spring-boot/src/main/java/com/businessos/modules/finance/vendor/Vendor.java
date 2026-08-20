package com.businessos.modules.finance.vendor;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * A supplier the company buys from - the master record real Accounts Payable needs.
 * Previously "vendor" was just a free-text string on Expense, so two spellings of the
 * same supplier were unrelated and there was no contact info, terms, or spend history.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "vendors", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vendor extends BaseEntity {

    private Long companyId; // Tenant isolation

    @Column(nullable = false)
    private String name;

    private String contactPerson;
    private String email;
    private String phone;
    private String taxId; // Vendor's VAT/BIN/TIN
    private String address;

    // Free text like "NET_30" / "Due on receipt" - informational for now
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private boolean active = true;
}
