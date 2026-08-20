package com.businessos.modules.servicedesk.task;

import com.businessos.enums.ServiceRequestPriority;
import com.businessos.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;       // was LocalDateTime — fixed to match Task.dueDate
import java.time.LocalDateTime;


@Getter
@Setter
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private ServiceRequestPriority priority;
    private LocalDate dueDate;              // fixed: was LocalDateTime
    private LocalDateTime slaDeadline;
    private LocalDateTime completedAt;
    private Long serviceRequestId;
    private Long assignedEmployeeId;
    private String assignedEmployeeName;
    private String assignedEmployeeRole;
    private Double estimatedHours;
    private Long createdById;
    private String createdByName;
    private Long workflowStageId;
    private String workflowStageName;
    private LocalDateTime createdAt;
}
