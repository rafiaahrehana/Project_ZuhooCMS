package com.businessos.modules.ai.prompt;

import com.businessos.modules.ai.exception.AiPromptException;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;


@Setter
@Accessors(chain = true)
public class InvoiceSummaryPromptBuilder {

    private String     clientName;
    private String     serviceName;
    private BigDecimal amount;
    private String     currency;
    private String     period;

    public static InvoiceSummaryPromptBuilder builder() {
        return new InvoiceSummaryPromptBuilder();
    }

    public String build() {
        validateFields();
        return """
            Write a concise, professional invoice summary note for the following service.

            Client Name  : %s
            Service      : %s
            Amount       : %s %s
            Period       : %s

            Output instructions:
            - Write 2–3 sentences suitable for the invoice notes field.
            - Confirm what the invoice covers and the amount due.
            - Return only the summary note — no preamble, no explanation.
            """.formatted(
                clientName,
                serviceName,
                amount.toPlainString(),
                PromptSupport.orDefault(currency, "BDT"),
                PromptSupport.orDefault(period, "Current period")
            );
    }


    private void validateFields() {
        PromptSupport.requireNonBlank(clientName, "clientName", "invoice summary");
        PromptSupport.requireNonBlank(serviceName, "serviceName", "invoice summary");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new AiPromptException("amount must be greater than zero for invoice summary prompt");
    }
}
