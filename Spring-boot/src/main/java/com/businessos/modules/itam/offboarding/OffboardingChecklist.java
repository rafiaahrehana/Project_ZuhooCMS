package com.businessos.modules.itam.offboarding;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "offboarding_checklists")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OffboardingChecklist extends BaseEntity {

    @Column(name = "company_id", nullable = false)
    private Long companyId;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private LocalDate offboardingDate;
    private LocalDate targetCompletionDate;

    // Hardware
    @Builder.Default
    private boolean hardwareCollected = false;
    private LocalDate hardwareCollectedDate;
    private String hardwareCollectedBy;
    private String hardwareNotes;

    // Software Licenses
    @Builder.Default
    private boolean licensesRevoked = false;
    private LocalDate licensesRevokedDate;
    private String licensesNotes;

    // Access Revocation
    @Builder.Default
    private boolean accessRevoked = false;
    private LocalDate accessRevokedDate;
    private String accessNotes;

    // Data Handover
    @Builder.Default
    private boolean dataHandedOver = false;
    private LocalDate dataHandoverDate;
    private String dataHandoverNotes;

    // Exit Interview
    @Builder.Default
    private boolean exitInterviewCompleted = false;
    private LocalDate exitInterviewDate;
    private String exitInterviewNotes;

    // Overall Status
    @Builder.Default
    private boolean completed = false;
    private LocalDate completionDate;
    private String completedBy;
    private String overallNotes;

    public int getCompletionPercentage() {
        int total = 5;
        int completed = 0;
        if (hardwareCollected) completed++;
        if (licensesRevoked) completed++;
        if (accessRevoked) completed++;
        if (dataHandedOver) completed++;
        if (exitInterviewCompleted) completed++;
        return (completed * 100) / total;
    }

    public boolean isAllTasksCompleted() {
        return hardwareCollected && licensesRevoked && accessRevoked && dataHandedOver && exitInterviewCompleted;
    }
}