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

import java.util.ArrayList;
import java.util.List;

@Entity(name = "PortalProject")
@Table(name = "website_projects")
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalProject extends BaseEntity {
    @Column(name = "company_id", nullable = false)
    private Long companyId;
    private String title;
    @Column(length = 500)
    private String summary;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
    private String coverImageUrl;
    private String client;
    private String category;
    private int year;

    @ElementCollection
    @CollectionTable(name = "website_project_tags", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
