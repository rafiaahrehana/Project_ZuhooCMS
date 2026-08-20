package com.businessos.modules.ai.tool.impl;

import com.businessos.modules.ai.tool.AiTool;
import com.businessos.modules.ai.tool.AiToolResult;
import com.businessos.modules.hrm.attendance.timesheet.TimesheetRequest;
import com.businessos.modules.hrm.attendance.timesheet.TimesheetResponse;
import com.businessos.modules.hrm.attendance.timesheet.TimesheetService;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LogTimesheetTool implements AiTool {

    private final TimesheetService timesheetService;

    @Override
    public String name() {
        return "log_timesheet";
    }

    @Override
    public String description() {
        return "Log a timesheet entry for the employee: what they worked on, which project, and how many hours.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "workDate", Map.of("type", "string", "format", "date", "description", "Defaults to today"),
                "hoursWorked", Map.of("type", "number"),
                "projectName", Map.of("type", "string"),
                "taskDescription", Map.of("type", "string")
            ),
            "required", java.util.List.of("hoursWorked", "taskDescription")
        );
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public String describeProposal(Map<String, Object> args) {
        Object date = args.getOrDefault("workDate", "today");
        return "log " + args.get("hoursWorked") + " hours on " + date
            + " for \"" + args.get("taskDescription") + "\""
            + (args.get("projectName") != null ? " (project: " + args.get("projectName") + ")" : "");
    }

    @Override
    public AiToolResult execute(Map<String, Object> args, Long userId, Long companyId) {
        if (args == null || args.get("hoursWorked") == null || args.get("taskDescription") == null) {
            return AiToolResult.failure("Missing hours worked or a description of what was worked on.");
        }

        TimesheetRequest request = new TimesheetRequest();
        try {
            request.setWorkDate(args.get("workDate") != null
                ? LocalDate.parse(args.get("workDate").toString()) : LocalDate.now());
            request.setHoursWorked(Double.parseDouble(args.get("hoursWorked").toString()));
        } catch (Exception e) {
            return AiToolResult.failure("Couldn't understand the date or hours: " + e.getMessage());
        }
        request.setTaskDescription(args.get("taskDescription").toString());
        if (args.get("projectName") != null) request.setProjectName(args.get("projectName").toString());

        try {
            TimesheetResponse response = timesheetService.log(request);
            return AiToolResult.ok(
                "Logged " + request.getHoursWorked() + " hours on " + request.getWorkDate()
                    + " for \"" + request.getTaskDescription() + "\" (entry #" + response.getId() + ").",
                response);
        } catch (BadRequestException e) {
            return AiToolResult.failure("Couldn't log that entry: " + e.getMessage());
        }
    }
}
