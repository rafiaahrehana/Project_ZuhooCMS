package com.businessos.modules.hrm.recruitment.candidate;

import com.businessos.core.base.BaseEntity;
import com.businessos.enums.ApplicationSource;
import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
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
 * The person, distinct from any one application. One candidate can apply to
 * several job postings over time - each of those is its own JobApplication
 * row pointing back here.
 */
@Entity
@Table(name = "recruitment_candidates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Candidate extends BaseEntity {

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

    private String portfolioUrl;

    @Column(length = 150)
    private String currentTitle;

    /** Comma-separated skill tags - same convention as TalentPoolCandidate.skills. */
    @Column(length = 500)
    private String skills;

    /** How this person first reached the company - set once, on first application. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ApplicationSource source;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Set once, on first application, when an employee referred this candidate via the AI agent's refer_candidate tool. Null for every other source. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_employee_id")
    private Employee referredByEmployee;
}
