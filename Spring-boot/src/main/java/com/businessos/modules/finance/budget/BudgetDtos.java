package com.businessos.modules.finance.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class BudgetDtos {

    @Data
    public static class BudgetRequest {
        @NotBlank(message = "Category is required")
        private String category;
        @NotNull(message = "Fiscal year is required")
        private Integer fiscalYear;
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        private BigDecimal amount;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BudgetResponse {
        private Long id;
        private String category;
        private int fiscalYear;
        private BigDecimal amount;
        private String notes;
        // Live budget-vs-actual rollup
        private BigDecimal actualSpend;
        private BigDecimal remaining;
        private double usedPercent;
        private boolean overBudget;
    }
}
