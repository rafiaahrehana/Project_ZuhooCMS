package com.businessos.modules.servicedesk.task;

import com.businessos.enums.ServiceRequestPriority;
import com.businessos.enums.TaskStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
public class UpdateTaskRequest {
    @Size(max = 255)
    private String title;
    private String description;
    private TaskStatus status;
    private ServiceRequestPriority priority;
    private LocalDate dueDate;              // fixed: was LocalDateTime
    private LocalDateTime slaDeadline;
    private Long assignedEmployeeId;
    private Double estimatedHours;
}
