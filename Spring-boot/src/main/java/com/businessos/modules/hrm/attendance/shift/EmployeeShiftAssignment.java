package com.businessos.modules.hrm.attendance.shift;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.time.LocalDate;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "employee_shift_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeShiftAssignment extends BaseEntity {

    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private Shift shift;

    private LocalDate assignmentStartDate;
    private LocalDate assignmentEndDate;

    @Builder.Default
    private boolean active = true;

    private String reason;
    private String assignedBy;

    private String notes;
}
