package com.businessos.modules.crm.tag;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * A shared tag taxonomy across Lead/Opportunity/Client within one company -
 * one vocabulary reused everywhere, not free text per record (see Client.tags,
 * which remains a legacy free-text field for backward compatibility).
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "tags", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tag_company_name", columnNames = {"company_id", "name"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tag extends BaseEntity {

    @Column(nullable = false, length = 60)
    private String name;

    // Hex color for the chip, e.g. "#8352ED"
    @Column(nullable = false, length = 20)
    private String color;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
