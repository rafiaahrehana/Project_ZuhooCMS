package com.businessos.modules.servicedesk.task;

import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.servicedesk.servicerequest.ServiceRequest;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStage;
import com.businessos.core.base.BaseEntity;
import com.businessos.enums.ServiceRequestPriority;
import com.businessos.modules.company.Company;
import com.businessos.auth.user.User;
import com.businessos.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDate;
import java.time.LocalDateTime;


@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Task extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ServiceRequestPriority priority = ServiceRequestPriority.NORMAL;

    private LocalDate dueDate;
    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Double estimatedHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private Employee assignedEmployee;

    private LocalDateTime slaDeadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_stage_id")
    private WorkflowStage workflowStage;
}