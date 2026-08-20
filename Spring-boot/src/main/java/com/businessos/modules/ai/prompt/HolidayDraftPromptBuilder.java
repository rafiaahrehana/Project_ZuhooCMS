package com.businessos.modules.ai.prompt;

import com.businessos.modules.ai.exception.AiPromptException;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Setter
@Accessors(chain = true)
public class HolidayDraftPromptBuilder {

    private String companyName;
    private LocalDate today;
    private String instructions;

    public static HolidayDraftPromptBuilder builder() {
        return new HolidayDraftPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Draft a single company holiday entry for %s. Today's date is %s.

            Instructions from the requester:
            %s

            Output instructions:
            - Respond with ONLY valid JSON, no markdown code fences, no explanation before or after.
            - Shape exactly: {"name": "...", "date": "YYYY-MM-DD", "type": "...", "description": "..."}
            - "name" is a short, clear holiday name (max 150 characters).
            - "date" must be a real calendar date in ISO format (YYYY-MM-DD). Infer the year from today's date if not specified.
            - "type" must be exactly one of: NATIONAL, RELIGIOUS, OPTIONAL, COMPANY.
            - "description" is one or two sentences, professional tone, ready to publish as-is.
            """.formatted(companyName, today, instructions);
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(companyName, "companyName", "holiday draft");
        if (today == null) throw new AiPromptException("today is required for holiday draft prompt");
        PromptSupport.requireNonBlank(instructions, "instructions", "holiday draft");
    }
}
