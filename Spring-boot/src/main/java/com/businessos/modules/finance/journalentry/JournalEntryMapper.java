package com.businessos.modules.finance.journalentry;

import java.util.List;
import java.util.stream.Collectors;

public class JournalEntryMapper {

    public static JournalEntryResponse toResponse(JournalEntry entity) {
        if (entity == null) {
            return null;
        }

        List<JournalEntryLineResponse> lines;
        if (entity.getLines() != null && !entity.getLines().isEmpty()) {
            lines = entity.getLines().stream().map(JournalEntryMapper::toLineResponse).collect(Collectors.toList());
        } else {
            // Pre-multi-line entry: synthesize the two lines from the legacy columns so
            // old and new entries render identically in the UI.
            lines = List.of(
                    JournalEntryLineResponse.builder()
                            .accountId(entity.getDebitAccount() != null ? entity.getDebitAccount().getId() : null)
                            .accountCode(entity.getDebitAccount() != null ? entity.getDebitAccount().getAccountCode() : null)
                            .accountName(entity.getDebitAccount() != null ? entity.getDebitAccount().getAccountName() : null)
                            .debitAmount(entity.getAmount())
                            .creditAmount(java.math.BigDecimal.ZERO)
                            .build(),
                    JournalEntryLineResponse.builder()
                            .accountId(entity.getCreditAccount() != null ? entity.getCreditAccount().getId() : null)
                            .accountCode(entity.getCreditAccount() != null ? entity.getCreditAccount().getAccountCode() : null)
                            .accountName(entity.getCreditAccount() != null ? entity.getCreditAccount().getAccountName() : null)
                            .debitAmount(java.math.BigDecimal.ZERO)
                            .creditAmount(entity.getAmount())
                            .build());
        }

        return JournalEntryResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .journalEntryNumber(entity.getJournalEntryNumber())
                .entryDate(entity.getEntryDate())
                .lines(lines)
                .debitAccountId(entity.getDebitAccount() != null ? entity.getDebitAccount().getId() : null)
                .debitAccountName(entity.getDebitAccount() != null ? entity.getDebitAccount().getAccountName() : null)
                .creditAccountId(entity.getCreditAccount() != null ? entity.getCreditAccount().getId() : null)
                .creditAccountName(entity.getCreditAccount() != null ? entity.getCreditAccount().getAccountName() : null)
                .amount(entity.getAmount())
                .description(entity.getDescription())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .approvedBy(entity.getApprovedBy())
                .approvedDate(entity.getApprovedDate())
                .approved(entity.isApproved())
                .posted(entity.isPosted())
                .postedDate(entity.getPostedDate())
                .reversed(entity.isReversed())
                .reversalEntryId(entity.getReversalEntryId())
                .reversedFromEntryId(entity.getReversedFromEntryId())
                .reversedDate(entity.getReversedDate())
                .build();
    }

    private static JournalEntryLineResponse toLineResponse(JournalEntryLine line) {
        return JournalEntryLineResponse.builder()
                .id(line.getId())
                .accountId(line.getAccount() != null ? line.getAccount().getId() : null)
                .accountCode(line.getAccount() != null ? line.getAccount().getAccountCode() : null)
                .accountName(line.getAccount() != null ? line.getAccount().getAccountName() : null)
                .debitAmount(line.getDebitAmount())
                .creditAmount(line.getCreditAmount())
                .lineDescription(line.getLineDescription())
                .build();
    }
}
