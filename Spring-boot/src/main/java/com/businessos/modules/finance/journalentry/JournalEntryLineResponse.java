package com.businessos.modules.finance.journalentry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryLineResponse {
    private Long id;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String lineDescription;
}
