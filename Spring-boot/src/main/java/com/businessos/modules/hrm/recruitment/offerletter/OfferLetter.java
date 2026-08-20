package com.businessos.modules.hrm.recruitment.offerletter;

import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.recruitment.jobapplication.JobApplication;
import com.businessos.core.base.BaseEntity;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import com.businessos.enums.LetterType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "employment_letters", uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "reference_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfferLetter extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LetterType letterType;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String signedBy;

    private LocalDate issueDate;

    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private boolean issued = false;
    @Builder.Default
    private boolean acknowledged = false;

    // Recipient — an existing Employee for employment letters, OR a recruitment
    // candidate (JobApplication) for pre-employment OFFER/APPOINTMENT letters.
    // Exactly one of the two is set; recipientName/Email are denormalized so the
    // stored letter and its PDF keep the recipient even if the source record changes.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_application_id")
    private JobApplication jobApplication;

    private String recipientName;
    private String recipientEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;
}
