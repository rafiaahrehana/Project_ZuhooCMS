package com.businessos.modules.finance.generalledger;

public class GeneralLedgerMapper {
    public static GeneralLedgerResponse toResponse(GeneralLedger entity) {
        if (entity == null) {
            return null;
        }

        return GeneralLedgerResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .transactionDate(entity.getTransactionDate())
                .accountId(entity.getAccount() != null ? entity.getAccount().getId() : null)
                .accountName(entity.getAccount() != null ? entity.getAccount().getAccountName() : null)
                .accountCode(entity.getAccount() != null ? entity.getAccount().getAccountCode() : null)
                .accountType(entity.getAccount() != null ? entity.getAccount().getType() : null)
                .debitAmount(entity.getDebitAmount())
                .creditAmount(entity.getCreditAmount())
                .description(entity.getDescription())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .referenceNumber(entity.getReferenceNumber())
                .isReconciled(entity.isReconciled())
                .reconciliationNotes(entity.getReconciliationNotes())
                .postedBy(entity.getPostedBy())
                .postedDate(entity.getPostedDate())
                .posted(entity.isPosted())
                .build();
    }
}
