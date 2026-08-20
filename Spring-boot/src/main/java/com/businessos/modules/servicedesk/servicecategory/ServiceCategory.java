package com.businessos.modules.servicedesk.servicecategory;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * FIXES:
 * 1. Added @Builder @AllArgsConstructor — controller uses ServiceCategory.builder()
 * 2. Added 'sortOrder' field — controller, mapper, response, and repository all reference it
 * 3. Added 'companyId' — categories are per-company (each company builds its own service
 *    catalog), not a shared platform-wide taxonomy. Name uniqueness is now scoped to the
 *    company instead of globally unique.
 */
@Entity
@Table(name = "service_categories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceCategory extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String iconUrl;

    @Builder.Default
    private boolean active = true;

    private String nameBn;
    private String descriptionBn;

    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
