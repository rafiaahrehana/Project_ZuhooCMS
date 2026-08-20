package com.businessos.modules.servicedesk.kb;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.servicedesk.servicecategory.ServiceCategory;
import com.businessos.modules.servicedesk.servicetemplate.ServiceTemplate;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kb_articles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KbArticle extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String title;

    private String summary;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KbArticleStatus status = KbArticleStatus.DRAFT;

    @Builder.Default
    private boolean clientVisible = false;

    private String keywords;

    @Builder.Default
    private int viewCount = 0;

    @Builder.Default
    private int helpfulCount = 0;

    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_service_id")
    private ServiceTemplate relatedService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;
}
