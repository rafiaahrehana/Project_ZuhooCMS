package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(chain = true)
public class WorkflowSuggestionPromptBuilder {

    private String goal;
    private String existingTemplatesSummary;

    public static WorkflowSuggestionPromptBuilder builder() {
        return new WorkflowSuggestionPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Suggest a service workflow (an ordered list of stages) for this company.

            What the company wants the workflow for:
            %s

            The company's existing workflow templates (for consistency - reuse similar
            stage naming and structure where it makes sense, avoid duplicating an
            already-covered process):
            %s

            Output instructions:
            - Respond with ONLY a JSON object, no markdown fences, no prose before or after.
            - Exact shape: {"name": "<short workflow name>", "stages": [{"name": "<stage name>", "purpose": "<one line>", "needsApproval": true|false}]}
            - 3 to 7 stages, in execution order.
            """.formatted(
                goal,
                PromptSupport.orDefault(existingTemplatesSummary, "No workflow templates configured yet")
            );
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(goal, "goal", "workflow suggestion");
    }
}
