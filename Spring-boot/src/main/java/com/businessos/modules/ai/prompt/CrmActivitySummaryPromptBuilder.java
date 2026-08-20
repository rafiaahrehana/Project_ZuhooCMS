package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class CrmActivitySummaryPromptBuilder {

    private String recordType;
    private String recordName;
    private String stageOrStatus;
    private String activityHistory;

    public static CrmActivitySummaryPromptBuilder builder() {
        return new CrmActivitySummaryPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Summarise the recent activity history for this CRM %s and suggest the next best action.

            %s Name        : %s
            Current Stage/Status : %s
            Activity History     :
            %s

            Output instructions:
            - Provide a 2-3 sentence summary of engagement so far.
            - Recommend one specific next action for the account owner.
            - Return only the summary and recommendation - no preamble.
            """.formatted(
                recordType,
                recordType,
                recordName,
                stageOrStatus,
                PromptSupport.orDefault(activityHistory, "No activity recorded yet")
            );
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(recordType, "recordType", "CRM activity summary");
        PromptSupport.requireNonBlank(recordName, "recordName", "CRM activity summary");
        PromptSupport.requireNonBlank(stageOrStatus, "stageOrStatus", "CRM activity summary");
    }
}
