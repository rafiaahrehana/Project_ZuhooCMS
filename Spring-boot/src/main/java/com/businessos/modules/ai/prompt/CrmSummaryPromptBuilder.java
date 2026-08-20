package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class CrmSummaryPromptBuilder {

    private String contactName;
    private String companyName;
    private String currentStatus;
    private String activityHistory;
    private String interestedService;

    public static CrmSummaryPromptBuilder builder() {
        return new CrmSummaryPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Summarise the following CRM lead and suggest the next best action.

            Contact Name       : %s
            Company            : %s
            Current Status     : %s
            Interested Service : %s
            Activity History   :
            %s

            Output instructions:
            - Provide a 2–3 sentence summary of the lead's engagement so far.
            - Recommend one specific next action for the sales team.
            - Return only the summary and recommendation — no preamble.
            """.formatted(
                contactName,
                PromptSupport.orDefault(companyName, "Unknown"),
                currentStatus,
                PromptSupport.orDefault(interestedService, "Not specified"),
                PromptSupport.orDefault(activityHistory, "No activity recorded yet")
            );
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(contactName, "contactName", "CRM summary");
        PromptSupport.requireNonBlank(currentStatus, "currentStatus", "CRM summary");
    }
}
