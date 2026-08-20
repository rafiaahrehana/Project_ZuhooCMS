package com.businessos.modules.hrm.recruitment.jobpost;

import com.businessos.modules.hrm.department.Department;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.enums.EducationLevel;
import com.businessos.enums.EmploymentType;
import com.businessos.enums.JobPostingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "job_postings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobPosting extends BaseEntity {

    @Column(nullable = false)
    private String title;

    private String jobTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobPostingStatus status = JobPostingStatus.DRAFT;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Builder.Default
    private Integer vacancies = 1;
    private LocalDate deadline;
    private String location;
    @Builder.Default
    private Boolean remote = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private Employee createdBy;

    /** The recruiter working this posting - reassignable, unlike createdBy which is a fixed creation record. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_recruiter_id")
    private Employee assignedRecruiter;

    // ── ATS matching requirements (all optional) ─────────────────
    // Drives CvScoringService's automated match score - a posting with none
    // of these set simply never gets scored (AtsParseStatus.NOT_APPLICABLE).
    /** Comma-separated skill tags - same convention as Candidate.skills. Weighted 40% of the ATS match score. */
    @Column(length = 500)
    private String requiredSkills;

    /** Comma-separated - nice-to-have skills and certifications together. Weighted 20% of the ATS match score. */
    @Column(length = 500)
    private String preferredSkills;

    /** Weighted 25% of the ATS match score, against years detected in the resume text. */
    private Integer minExperienceYears;

    /** Weighted 15% of the ATS match score, against the highest degree level detected in the resume text. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EducationLevel minEducationLevel;
}
