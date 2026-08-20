package com.businessos.modules.website;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
@Table(name = "website_services")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service extends BaseEntity {
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    @Column(unique = true)
    private String slug;
    private String title;
    @Column(length = 500)
    private String summary;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
    private String icon;
    private String imageUrl;
    private String categoryName;
    private String startingPrice;
    private String estimatedTime;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String requirements;

    @ElementCollection
    @CollectionTable(name = "website_service_features", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "feature")
    @Builder.Default
    private List<String> features = new ArrayList<>();
}
