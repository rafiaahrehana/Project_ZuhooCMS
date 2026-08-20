package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Builds the prompt for AI-powered search answers.
 * The question is the platformuser's natural-language query; context is a
 * plain-text digest of the top search results across modules.
 */
@Setter
@Accessors(chain = true)
public class SearchAnswerPromptBuilder {

    private String question;
    private String context;

    public static SearchAnswerPromptBuilder builder() {
        return new SearchAnswerPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Answer the user's question using ONLY the business records below.

            Question: %s

            Business records:
            %s

            Output instructions:
            - Answer in 2-4 sentences based strictly on the records above.
            - If the records do not contain the answer, say so plainly.
            - Do not invent names, numbers, or statuses that are not in the records.
            - Return only the answer — no preamble.
            """.formatted(question, PromptSupport.orDefault(context, "No matching records found"));
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(question, "question", "search answer");
    }
}
