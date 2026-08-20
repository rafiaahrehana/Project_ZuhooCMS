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

/**
 * Groups a run of AiConversation messages into one resumable chat, and (for
 * the agent loop) carries the one pending write-action awaiting the
 * employee's confirmation. Messages themselves still live on AiConversation
 * (thread is a nullable FK there) so every pre-existing generateRaw() caller
 * that never threads its calls keeps working unchanged.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "ai_conversation_threads",
    indexes = {
        @Index(name = "idx_ai_thread_company_user", columnList = "company_id, user_id"),
    }
)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
public class AiConversationThread extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiFeature feature;

    // First ~60 chars of the thread's first message - set once, never edited,
    // so the thread list has something readable without re-fetching messages.
    @Column(length = 80)
    private String title;

    // Non-null only while a write-tool proposal is awaiting the employee's
    // next "yes"/"no" - JSON of {tool, args}. Cleared after every agent turn
    // whether confirmed, cancelled, or superseded by a new question.
    @Column(name = "pending_action", columnDefinition = "TEXT")
    private String pendingAction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
