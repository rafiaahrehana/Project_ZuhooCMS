package com.businessos.modules.hrm.leave.leavebalance;

import com.businessos.modules.hrm.employee.Employee;
import com.businessos.core.base.BaseEntity;
import com.businessos.modules.company.Company;
import com.businessos.enums.LeaveType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "leave_balances",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type", "year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LeaveBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeaveType leaveType;

    private int year;

    @Builder.Default
    private int totalDays     = 0;
    @Builder.Default
    private int usedDays      = 0;
    @Builder.Default
    private int pendingDays   = 0;

    public int getRemainingDays() {
        return Math.max(0, totalDays - usedDays - pendingDays);
    }
}
