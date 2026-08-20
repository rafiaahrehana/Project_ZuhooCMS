package com.businessos.modules.finance.expense;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExpenseRequest {

    private String title;
    private String currency;
    private Long employeeId;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    private String vendorName;
    private String category;

    // Optional COA account (must be EXPENSE type) this expense posts to when paid.
    private Long expenseAccountId;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    private String receiptUrl;
    private String notes;
    private String referenceNumber;
}
