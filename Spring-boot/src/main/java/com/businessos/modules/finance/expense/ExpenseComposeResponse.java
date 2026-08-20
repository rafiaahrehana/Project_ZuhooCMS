package com.businessos.modules.finance.expense;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseComposeResponse {
    private String title;
    private String description;
}
