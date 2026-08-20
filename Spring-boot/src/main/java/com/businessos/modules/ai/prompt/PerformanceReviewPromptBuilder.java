package com.businessos.modules.ai.prompt;

import com.businessos.modules.ai.exception.AiPromptException;
import lombok.Setter;
import lombok.experimental.Accessors;


@Setter
@Accessors(chain = true)
public class PerformanceReviewPromptBuilder {

    private String employeeName;
    private String designation;
    private String reviewPeriod;
    private int overallScore;
    private String strengths;
    private String areasForImprovement;
    private String goalsForNextPeriod;

    public static PerformanceReviewPromptBuilder builder() {
        return new PerformanceReviewPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Write a professional performance review summary for the following employee.

            Employee Name       : %s
            Designation         : %s
            Review Period       : %s
            Overall Score (1-5) : %d
            Strengths           : %s
            Areas to Improve    : %s
            Goals Next Period   : %s

            Output instructions:
            - Write in third person.
            - Keep the tone professional and constructive.
            - Return only the review summary — no preamble, no explanation.
            """.formatted(
                employeeName,
                PromptSupport.orDefault(designation, "Not specified"),
                reviewPeriod,
                overallScore,
                PromptSupport.orDefault(strengths, "Not specified"),
                PromptSupport.orDefault(areasForImprovement, "Not specified"),
                PromptSupport.orDefault(goalsForNextPeriod, "Not specified")
            );
    }

    private void validateFields() {
        PromptSupport.requireNonBlank(employeeName, "employeeName", "performance review");
        PromptSupport.requireNonBlank(reviewPeriod, "reviewPeriod", "performance review");
        if (overallScore < 1 || overallScore > 5)
            throw new AiPromptException("overallScore must be between 1 and 5");
    }
}
