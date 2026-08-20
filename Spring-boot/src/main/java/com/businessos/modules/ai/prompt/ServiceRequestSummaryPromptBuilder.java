package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class ServiceRequestSummaryPromptBuilder {

    private String title;
    private String description;
    private String status;
    private String priority;
    private String clientName;
    private String assignedEmployeeName;
    private String taskProgress;
    private boolean slaBreach;
    private String recentComments;

    public static ServiceRequestSummaryPromptBuilder builder() {
        return new ServiceRequestSummaryPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Summarise the following service request and suggest the next best action.

            Title              : %s
            Description        : %s
            Status             : %s
            Priority           : %s
            Client             : %s
            Assigned To        : %s
            Task Progress      : %s
            SLA Breached       : %s
            Recent Comments    :
            %s

            Output instructions:
            - Provide a 2-3 sentence summary of where this request stands.
            - Recommend one specific next action for the assigned team member.
            - Return only the summary and recommendation - no preamble.
            """.formatted(
                title,
                PromptSupport.orDefault(description, "Not provided"),
                status,
                PromptSupport.orDefault(priority, "Not set"),
                PromptSupport.orDefault(clientName, "Unknown"),
                PromptSupport.orDefault(assignedEmployeeName, "Unassigned"),
                PromptSupport.orDefault(taskProgress, "No tasks yet"),
                slaBreach ? "Yes" : "No",
                PromptSupport.orDefault(recentComments, "No comments yet")
            );
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(title, "title", "service request summary");
        PromptSupport.requireNonBlank(status, "status", "service request summary");
    }
}
