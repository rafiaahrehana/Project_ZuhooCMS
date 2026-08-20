package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class ExpenseEntryPromptBuilder {

    private String vendorName;
    private String amount;
    private String category;
    private String roughNotes;

    public static ExpenseEntryPromptBuilder builder() {
        return new ExpenseEntryPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Turn an employee's rough notes about a purchase into a clean expense claim entry.

            Vendor            : %s
            Amount            : %s
            Category          : %s
            Rough notes       : %s

            Output instructions:
            - Respond with ONLY valid JSON, no markdown code fences, no explanation before or after.
            - Shape exactly: {"title": "...", "description": "..."}
            - "title" is a short claim title (max 60 characters), e.g. "Client dinner - Acme Corp".
            - "description" is 1-2 sentences explaining the business purpose, professional tone, first person implied (no "I").
            """.formatted(
                PromptSupport.orDefault(vendorName, "Not specified"),
                PromptSupport.orDefault(amount, "Not specified"),
                PromptSupport.orDefault(category, "Not specified"),
                roughNotes
            );
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(roughNotes, "roughNotes", "expense entry");
    }
}
