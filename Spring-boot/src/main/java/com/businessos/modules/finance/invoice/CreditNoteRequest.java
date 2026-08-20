package com.businessos.modules.finance.invoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditNoteRequest {
    @NotNull(message = "Invoice ID is required")
    private Long clientInvoiceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String reason;
}
