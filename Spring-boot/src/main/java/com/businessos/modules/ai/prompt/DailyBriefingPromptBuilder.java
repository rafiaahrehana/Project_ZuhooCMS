package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class DailyBriefingPromptBuilder {

    private String employeeFirstName;
    private int assignedServiceRequestCount;
    private int slaBreachedCount;
    private int activeAnnouncementCount;
    private double weekHoursLogged;
    private String lowLeaveBalanceNote;

    public static DailyBriefingPromptBuilder builder() {
        return new DailyBriefingPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Write a short, warm "good morning" briefing for %s, based only on these real facts:

            - Service requests assigned to them right now: %d (of which %d are SLA-breached)
            - Active company announcements: %d
            - Hours logged so far this week: %.1f
            - Leave balance note: %s

            Output instructions:
            - 2-4 sentences, plain text, no markdown, no preamble like "Here is your briefing".
            - Friendly and direct, like a helpful colleague, not corporate.
            - Only mention a fact if it's actually notable (e.g. don't mention 0 SLA breaches as if it were news).
            - Do not invent any fact not given above.
            """.formatted(
                employeeFirstName,
                assignedServiceRequestCount,
                slaBreachedCount,
                activeAnnouncementCount,
                weekHoursLogged,
                PromptSupport.orDefault(lowLeaveBalanceNote, "nothing notable")
            );
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(employeeFirstName, "employeeFirstName", "daily briefing");
    }
}
