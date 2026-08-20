package com.businessos.modules.ai.entity;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(
    name = "ai_prompt_templates",
    indexes = {
        @Index(name = "idx_ai_tpl_company", columnList = "company_id"),
        @Index(name = "idx_ai_tpl_feature", columnList = "feature, active")
    }
)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
// Same reasoning as AiProviderConfig: a company_id IS NULL row is a platform
// default template shared with every tenant. findActiveForFeature documents and
// relies on that "company-specific wins, else platform default" fallback, but a
// strict "company_id = :companyId" filter silently deleted the IS NULL branch
// from the query, so the platform defaults were unreachable for tenant users.
@Filter(name = "tenantFilter", condition = "(company_id = :companyId or company_id is null)")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiPromptTemplate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiFeature feature;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String template;

    @Column(nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "change_notes", length = 500)
    private String changeNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;
}
