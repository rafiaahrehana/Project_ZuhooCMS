package com.businessos.modules.hrm.performance;

import com.businessos.auth.user.User;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * A supporting document attached to a performance review - appraisal forms,
 * certificates, client appreciation letters.
 *
 * Modelled on servicedesk's Document: the file itself is uploaded through the
 * shared POST /api/upload endpoint, which returns a URL. Only that URL and its
 * metadata are stored here, so no binary ever goes through this table.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "performance_review_attachments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PerformanceReviewAttachment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private PerformanceReview review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false)
    private String fileName;

    private String fileUrl;
    private String fileType;
    private Long fileSizeBytes;

    /** Optional human label, e.g. "Client Appreciation". */
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;
}
