package com.businessos.modules.ai.prompt;

import com.businessos.modules.ai.exception.AiPromptException;
import lombok.Setter;
import lombok.experimental.Accessors;


@Setter
@Accessors(chain = true)
public class LeavePolicyPromptBuilder {

    private String  companyName;
    private String  industry;
    private int     annualLeaveDays;
    private int     sickLeaveDays;
    private boolean remoteWorkAllowed;
    private String  additionalContext;

    public static LeavePolicyPromptBuilder builder() {
        return new LeavePolicyPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Draft an enterprise-grade company leave policy for the following organisation.

            Company Name        : %s
            Industry            : %s
            Annual Leave Days   : %d
            Sick Leave Days     : %d
            Remote Work Allowed : %s
            Additional Context  : %s

            Output instructions:
            - Organise into clear sections: Scope, Leave Types, Accrual Rules, Request and Approval Process.
            - Use formal language.
            - Return only the policy document — no preamble, no explanation.
            """.formatted(
                companyName,
                PromptSupport.orDefault(industry, "General"),
                annualLeaveDays,
                sickLeaveDays,
                remoteWorkAllowed ? "Yes" : "No",
                PromptSupport.orDefault(additionalContext, "None")
            );
    }


    private void validateFields() {
        PromptSupport.requireNonBlank(companyName, "companyName", "leave policy");
        if (annualLeaveDays <= 0)
            throw new AiPromptException("annualLeaveDays must be greater than zero");
    }
}
