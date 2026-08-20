package com.businessos.modules.finance.journalentry;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntryRequest {

    @NotNull(message = "Entry date is required")
    private LocalDate entryDate;

    /**
     * Multi-line form: at least 2 lines, total debits must equal total credits.
     * If omitted, the legacy single debit/credit fields below are used instead
     * (kept so older API callers keep working - the server synthesizes 2 lines).
     */
    @Valid
    private List<JournalEntryLineRequest> lines;

    // ── Legacy 1:1 form (used only when `lines` is absent) ──────────────
    private Long debitAccountId;
    private Long creditAccountId;
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String description;

    private String notes;
}
