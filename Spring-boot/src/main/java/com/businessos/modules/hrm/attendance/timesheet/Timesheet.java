package com.businessos.modules.hrm.attendance.timesheet;

import com.businessos.modules.hrm.employee.Employee;
import com.businessos.core.base.BaseEntity;
import com.businessos.auth.user.User;
import com.businessos.modules.company.Company;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "timesheets",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "work_date"}))
@Getter @Setter @NoArgsConstructor
public class Timesheet extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    private LocalTime startTime;
    private LocalTime endTime;
    private Double hoursWorked;
    private Double billableHours;

    @Column(columnDefinition = "TEXT")
    private String workSummary;

    @Column(name = "project_name", length = 150)
    private String projectName;

    @Column(name = "task_description", columnDefinition = "TEXT")
    private String taskDescription;

    private boolean submitted = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    private boolean approved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;
}
