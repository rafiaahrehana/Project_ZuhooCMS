package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class TimesheetEntryPromptBuilder {

    private String projectName;
    private String roughNotes;

    public static TimesheetEntryPromptBuilder builder() {
        return new TimesheetEntryPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Turn an employee's rough notes about their workday into a clean timesheet entry.

            Project           : %s
            Rough notes       : %s

            Output instructions:
            - Respond with ONLY valid JSON, no markdown code fences, no explanation before or after.
            - Shape exactly: {"taskDescription": "...", "description": "..."}
            - "taskDescription" is a short task/activity title (max 80 characters).
            - "description" is 1-2 sentences of what was actually done, professional tone, first person implied (no "I").
            """.formatted(
                PromptSupport.orDefault(projectName, "Not specified"),
                roughNotes
            );
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(roughNotes, "roughNotes", "timesheet entry");
    }
}
