package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.servicedesk.task.CreateTaskRequest;
import com.businessos.modules.servicedesk.task.TaskResponse;
import com.businessos.modules.servicedesk.task.TaskService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AssignTaskTool implements AiTool {

    private final TaskService taskService;
    private final EmployeeRepository employeeRepository;

    @Override
    public String name() {
        return "assign_task";
    }

    @Override
    public String description() {
        return "Add a task to a service request, optionally assigned to a specific employee - same as adding a task from the request's detail page.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "serviceRequestId", Map.of("type", "integer", "description", "The request's id (visible as # in its listing)"),
                "taskTitle", Map.of("type", "string"),
                "assignedEmployeeName", Map.of("type", "string", "description", "Optional - who to assign the task to"),
                "dueDate", Map.of("type", "string", "format", "date")
            ),
            "required", List.of("serviceRequestId", "taskTitle")
        );
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public String describeProposal(Map<String, Object> args) {
        String base = "add a task \"" + args.get("taskTitle") + "\" to request #" + args.get("serviceRequestId");
        return args.get("assignedEmployeeName") != null ? base + ", assigned to " + args.get("assignedEmployeeName") : base;
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        if (args == null || args.get("serviceRequestId") == null || args.get("taskTitle") == null) {
            return AiToolResult.failure("I need the service request id and a task title.");
        }

        Long requestId;
        try {
            requestId = Long.valueOf(args.get("serviceRequestId").toString());
        } catch (NumberFormatException e) {
            return AiToolResult.failure("That doesn't look like a valid request id.");
        }

        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(args.get("taskTitle").toString());
        if (args.get("dueDate") != null) {
            try {
                request.setDueDate(LocalDate.parse(args.get("dueDate").toString()));
            } catch (Exception ignored) {
                // Bad date format from the model - proceed without one rather than failing the whole task.
            }
        }

        if (args.get("assignedEmployeeName") != null) {
            String name = args.get("assignedEmployeeName").toString();
            List<Employee> matches = employeeRepository
                .searchEmployeesWithoutStatus(companyId, null, null, name, PageRequest.of(0, 5))
                .getContent();
            if (matches.size() == 1) {
                request.setAssignedEmployeeId(matches.get(0).getId());
            } else if (matches.isEmpty()) {
                return AiToolResult.failure("I couldn't find an employee matching \"" + name + "\".");
            } else {
                return AiToolResult.failure("More than one employee matches \"" + name + "\" - please be more specific.");
            }
        }

        try {
            TaskResponse response = taskService.addTask(requestId, request);
            return AiToolResult.ok(
                "Added task \"" + response.getTitle() + "\" to request #" + requestId
                    + (response.getAssignedEmployeeName() != null ? ", assigned to " + response.getAssignedEmployeeName() : "") + ".",
                response);
        } catch (ResourceNotFoundException e) {
            return AiToolResult.failure("I couldn't find service request #" + requestId + ".");
        } catch (BadRequestException e) {
            return AiToolResult.failure("Couldn't add that task: " + e.getMessage());
        }
    }
}
