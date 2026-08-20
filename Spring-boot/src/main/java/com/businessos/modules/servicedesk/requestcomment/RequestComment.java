package com.businessos.modules.servicedesk.requestcomment;

import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.enums.CommentVisibility;
import com.businessos.core.base.BaseEntity;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;

/**
 * FIXES:
 * 1. 'message' → 'content'                (all callers use content)
 * 2. 'boolean internal' → 'CommentVisibility visibility'  (all callers use the enum)
 * 3. relation 'request' → 'serviceRequest'  (repository derived queries + service builder)
 * 4. added 'company' field               (service builder sets company)
 * 5. added @Builder                       (service uses RequestComment.builder())
 */
@Entity
@Table(name = "request_comments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequestComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CommentVisibility visibility = CommentVisibility.INTERNAL;

    private String attachmentUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
