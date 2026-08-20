package com.businessos.modules.ai.prompt;

import com.businessos.modules.ai.exception.AiPromptException;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Setter
@Accessors(chain = true)
public class AnnouncementDraftPromptBuilder {

    private String companyName;
    private LocalDate today;
    private String instructions;

    public static AnnouncementDraftPromptBuilder builder() {
        return new AnnouncementDraftPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Draft an internal company announcement for %s, dated %s.

            Instructions from the requester:
            %s

            Output instructions:
            - Respond with ONLY valid JSON, no markdown code fences, no explanation before or after.
            - Shape exactly: {"title": "...", "body": "..."}
            - "title" is a short, clear headline (max 80 characters).
            - "body" is the full announcement text, professional tone, ready to publish as-is.
            """.formatted(companyName, today, instructions);
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(companyName, "companyName", "announcement draft");
        if (today == null) throw new AiPromptException("today is required for announcement draft prompt");
        PromptSupport.requireNonBlank(instructions, "instructions", "announcement draft");
    }
}
