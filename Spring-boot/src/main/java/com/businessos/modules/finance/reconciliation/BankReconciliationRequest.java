package com.businessos.modules.finance.reconciliation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankReconciliationRequest {

    @NotNull
    private Long bankAccountId;

    @NotNull
    private BigDecimal bankStatementBalance;
}
