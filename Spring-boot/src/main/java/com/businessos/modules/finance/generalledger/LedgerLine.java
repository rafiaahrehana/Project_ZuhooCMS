package com.businessos.modules.finance.generalledger;

import java.math.BigDecimal;

/**
 * One line of a balanced multi-line posting - see GeneralLedgerService.recordBalancedTransaction().
 * Exactly one of debitAmount/creditAmount should be nonzero per line (a line can be zero/zero,
 * it'll just be skipped, but never both nonzero on the same line).
 */
public record LedgerLine(Long accountId, BigDecimal debitAmount, BigDecimal creditAmount) {

    public static LedgerLine debit(Long accountId, BigDecimal amount) {
        return new LedgerLine(accountId, amount, BigDecimal.ZERO);
    }

    public static LedgerLine credit(Long accountId, BigDecimal amount) {
        return new LedgerLine(accountId, BigDecimal.ZERO, amount);
    }
}
