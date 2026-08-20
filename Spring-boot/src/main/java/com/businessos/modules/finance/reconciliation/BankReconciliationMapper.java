package com.businessos.modules.finance.reconciliation;

import java.math.BigDecimal;

public class BankReconciliationMapper {

    public static BankReconciliationResponse toResponse(BankReconciliation entity) {
        if (entity == null) {
            return null;
        }

        BigDecimal deposits = entity.getOutstandingDepositsTotal() != null ? entity.getOutstandingDepositsTotal() : BigDecimal.ZERO;
        BigDecimal checks = entity.getOutstandingChecksTotal() != null ? entity.getOutstandingChecksTotal() : BigDecimal.ZERO;
        BigDecimal bankStatementBalance = entity.getBankStatementBalance() != null ? entity.getBankStatementBalance() : BigDecimal.ZERO;

        return BankReconciliationResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .bankAccountId(entity.getBankAccount() != null ? entity.getBankAccount().getId() : null)
                .bankAccountName(entity.getBankAccount() != null ? entity.getBankAccount().getAccountName() : null)
                .reconciliationDate(entity.getReconciliationDate())
                .glBalance(entity.getGlBalance())
                .bankStatementBalance(entity.getBankStatementBalance())
                .difference(entity.getDifference())
                .outstandingDepositsTotal(deposits)
                .outstandingChecksTotal(checks)
                .adjustedBankBalance(bankStatementBalance.add(deposits).subtract(checks))
                .reconciled(entity.isReconciled())
                .reconciledDate(entity.getReconciledDate())
                .reconciledBy(entity.getReconciledBy())
                .discrepancyNotes(entity.getDiscrepancyNotes())
                .statementFileName(entity.getStatementFileName())
                .statementFileUrl(entity.getStatementFileUrl())
                .statementUploadedAt(entity.getStatementUploadedAt())
                .build();
    }
}
