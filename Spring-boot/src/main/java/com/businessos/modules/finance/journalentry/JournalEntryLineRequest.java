package com.businessos.modules.finance.journalentry;

import jakarta.validation.constraints.DecimalMin;
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
public class JournalEntryLineRequest {

    @NotNull(message = "Account ID is required on every line")
    private Long accountId;

    @DecimalMin(value = "0.0")
    private BigDecimal debitAmount;

    @DecimalMin(value = "0.0")
    private BigDecimal creditAmount;

    private String lineDescription;
}
