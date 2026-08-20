package com.businessos.modules.hrm.recruitment.talentpool;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A qualified candidate kept for future openings - pooled from a closed
 * application (declined offer, good-but-no-vacancy) or added directly
 * (walk-ins, referrals, conference contacts).
 */
@Entity
@Table(name = "recruitment_talent_pool")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TalentPoolCandidate extends BaseEntity {

    public enum Reason { DECLINED_OFFER, NO_VACANCY, FUTURE_FIT, WITHDREW, REFERRAL, OTHER }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 200)
    private String email;

    private String phone;

    private String resumeUrl;

    private String linkedInUrl;

    @Column(length = 150)
    private String desiredRole;

    /** Comma-separated skill tags - searched, filtered, chip-rendered. */
    @Column(length = 500)
    private String skills;

    /** 1-5, how strong the candidate looked. */
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Reason reason = Reason.FUTURE_FIT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** The application this candidate came from, when pooled from one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_application_id")
    private JobApplication sourceApplication;
}
