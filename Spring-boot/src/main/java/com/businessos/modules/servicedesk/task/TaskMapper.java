package com.businessos.modules.servicedesk.task;

import com.businessos.auth.user.User;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.servicedesk.workflow.stage.WorkflowStage;

public class TaskMapper {

    public static TaskResponse toTaskResponse(Task task) {
        Employee assigned = task.getAssignedEmployee();
        User createdBy = task.getCreatedBy();
        WorkflowStage stage = task.getWorkflowStage();

        TaskResponse r = new TaskResponse();
        r.setId(task.getId());
        r.setTitle(task.getTitle());
        r.setDescription(task.getDescription());
        r.setStatus(task.getStatus());
        r.setPriority(task.getPriority());
        r.setDueDate(task.getDueDate());
        r.setSlaDeadline(task.getSlaDeadline());
        r.setCompletedAt(task.getCompletedAt());
        r.setEstimatedHours(task.getEstimatedHours());
        r.setServiceRequestId(task.getServiceRequest() != null ? task.getServiceRequest().getId() : null);
        r.setAssignedEmployeeId(assigned != null ? assigned.getId() : null);
        r.setAssignedEmployeeName(assigned != null && assigned.getUser() != null
                ? assigned.getUser().getFullName() : null);
        r.setCreatedById(createdBy != null ? createdBy.getId() : null);
        r.setCreatedByName(createdBy != null ? createdBy.getFullName() : null);
        r.setWorkflowStageId(stage != null ? stage.getId() : null);
        r.setWorkflowStageName(stage != null ? stage.getName() : null);
        r.setCreatedAt(task.getCreatedAt());
        return r;
    }

}
