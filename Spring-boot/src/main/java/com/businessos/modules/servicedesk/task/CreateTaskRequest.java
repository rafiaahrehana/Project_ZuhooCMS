package com.businessos.modules.servicedesk.task;

import com.businessos.enums.ServiceRequestPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CreateTaskRequest {

    @NotBlank(message = "Task title is required")
    @Size(max = 255)
    private String title;

    private String description;
    private ServiceRequestPriority priority;
    private LocalDate dueDate;
    private LocalDateTime slaDeadline;
    private Long assignedEmployeeId;
    private Long workflowStageId;
    private Double estimatedHours;
}
