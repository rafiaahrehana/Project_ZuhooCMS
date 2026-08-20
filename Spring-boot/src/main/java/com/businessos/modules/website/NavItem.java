package com.businessos.modules.website;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "website_nav_items")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavItem extends BaseEntity {
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    private String label;
    private String url;
    private boolean external;
    private Long parentId;
    private int sortOrder;
    private boolean mega;
}
