package com.businessos.modules.ai.prompt;

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Builds the prompt for the AI business-insights feature on the dashboard.
 * Metrics is a plain-text digest of the company's dashboard summary.
 */
@Setter
@Accessors(chain = true)
public class BusinessInsightsPromptBuilder {

    private String metrics;

    public static BusinessInsightsPromptBuilder builder() {
        return new BusinessInsightsPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            You are a business analyst. Review this company's current operating metrics
            and provide actionable insights.

            Current metrics:
            %s

            Output instructions:
            - Give 3 short, concrete insights (one sentence each).
            - Each insight must reference a specific number from the metrics.
            - Prioritise risks (SLA breaches, overdue invoices) over positives.
            - Return only the 3 insights as a numbered list — no preamble.
            """.formatted(metrics);
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(metrics, "metrics", "business insights");
    }
}
