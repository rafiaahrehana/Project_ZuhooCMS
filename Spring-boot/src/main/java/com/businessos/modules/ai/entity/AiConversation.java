package com.businessos.modules.ai.entity;

import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.enums.AiModel;
import com.businessos.modules.ai.enums.AiProviderType;
import com.businessos.modules.company.Company;
import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "ai_conversations",
    indexes = {
        @Index(name = "idx_ai_conv_company",  columnList = "company_id"),
        @Index(name = "idx_ai_conv_feature",  columnList = "company_id, feature")
    }
)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public class AiConversation extends BaseEntity {

    @Column(nullable = false, length = 36)
    private String conversationUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiFeature feature;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AiProviderType provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private AiModel model;

    @Column(columnDefinition = "TEXT")
    private String requestPayload;

    @Column(columnDefinition = "TEXT")
    private String responsePayload;

    private Long executionTimeMs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Nullable: only set when generate()/streamGenerate() was called with a
    // threadId. Every pre-existing generateRaw() caller (the *PromptBuilder
    // integrations) never sets this and is unaffected. requestPayload/
    // responsePayload already carry both sides of one exchange, so replaying
    // a thread's history is just reading these rows oldest-to-newest - no
    // separate per-message role column needed.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id")
    private AiConversationThread thread;

}
