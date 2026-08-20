package com.businessos.modules.website;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "website_pricing_plans")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingPlan extends BaseEntity {
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    private String name;
    @Column(length = 500)
    private String description;
    private String price;
    private String period;
    private String cta;
    private boolean featured;

    @ElementCollection
    @CollectionTable(name = "website_pricing_features", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "feature")
    @Builder.Default
    private List<String> features = new ArrayList<>();
}
