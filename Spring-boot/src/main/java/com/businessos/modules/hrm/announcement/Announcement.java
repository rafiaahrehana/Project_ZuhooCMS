package com.businessos.modules.hrm.announcement;

import com.businessos.core.base.BaseEntity;
import com.businessos.enums.AnnouncementAudience;
import com.businessos.auth.user.User;
import com.businessos.modules.hrm.department.Department;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Filter;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(
        name = "announcements",
        indexes = {
                @Index(name = "idx_announcement_company",   columnList = "company_id"),
                @Index(name = "idx_announcement_published", columnList = "company_id, published_at"),
                @Index(name = "idx_announcement_expires",   columnList = "expires_at")
        }
)
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private AnnouncementAudience audience = AnnouncementAudience.ALL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_department_id")
    private Department targetDepartment;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // When set on a still-unpublished announcement, AnnouncementScheduledPublishScheduler
    // auto-publishes it once this time passes - HR can draft a holiday notice
    // Friday to auto-publish Monday 9am instead of remembering to click Publish.
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(name = "priority", nullable = false)
    @Builder.Default
    private int priority = 0;

    @Column(name = "notify_all")
    @Builder.Default
    private boolean notifyAll = false;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;
}
