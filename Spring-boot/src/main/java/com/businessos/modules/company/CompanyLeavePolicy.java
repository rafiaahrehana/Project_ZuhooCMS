package com.businessos.modules.company;

import com.businessos.core.base.BaseEntity;
import com.businessos.enums.EmploymentType;
import com.businessos.enums.LeaveType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "company_leave_policies",
       uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "leave_type", "employment_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyLeavePolicy extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmploymentType employmentType;

    @Column(nullable = false)
    private int annualEntitlement;

    @Builder.Default
    private int maxCarryForward = 0;

    private Integer maxConsecutiveDays;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean requiresApproval = true;
    @Builder.Default
    private boolean canCarryForward = false;
    @Builder.Default
    private boolean paid = true;
    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private int applicableFromMonths = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
}
