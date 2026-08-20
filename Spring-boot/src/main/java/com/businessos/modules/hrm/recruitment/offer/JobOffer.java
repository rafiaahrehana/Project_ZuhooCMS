package com.businessos.modules.hrm.recruitment.offer;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The negotiation record of an offer: what was offered (title, salary
 * breakdown, joining date), when it expires, and what the candidate decided.
 * The printable document stays in the letters module - this is the data an
 * accepted offer hands to onboarding, using the same component fields as
 * HireApplicationRequest so nothing gets retyped.
 */
@Entity
@Table(name = "recruitment_job_offers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobOffer extends BaseEntity {

    public enum Status { DRAFT, SENT, ACCEPTED, DECLINED, WITHDRAWN }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id", nullable = false)
    private JobApplication jobApplication;

    @Column(nullable = false, length = 150)
    private String offeredJobTitle;

    private LocalDate joiningDate;

    /** After this date a SENT offer counts as expired (derived, not a stored status). */
    private LocalDate expiryDate;

    // ── Offered salary (monthly) ──────────────────────────────
    @Column(precision = 12, scale = 2)
    private BigDecimal grossSalary;

    @Column(precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(precision = 12, scale = 2)
    private BigDecimal houseRent;

    @Column(precision = 12, scale = 2)
    private BigDecimal medicalAllowance;

    @Column(precision = 12, scale = 2)
    private BigDecimal transportAllowance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    private LocalDateTime sentAt;

    private LocalDateTime decidedAt;

    @Column(columnDefinition = "TEXT")
    private String declineReason;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
