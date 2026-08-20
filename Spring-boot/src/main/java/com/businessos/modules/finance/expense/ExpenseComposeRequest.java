package com.businessos.modules.finance.expense;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExpenseComposeRequest {
    private String vendorName;
    private String amount;
    private String category;
    @NotBlank
    private String roughNotes;
}
