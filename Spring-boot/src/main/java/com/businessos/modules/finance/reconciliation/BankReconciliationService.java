package com.businessos.modules.finance.reconciliation;

import com.businessos.modules.finance.generalledger.GeneralLedgerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface BankReconciliationService {

    BankReconciliationResponse create(BankReconciliationRequest request);

    BankReconciliationResponse getById(Long id);

    Page<BankReconciliationResponse> getAll(Pageable pageable);

    /** GL entries on this reconciliation's account, dated on/before it, not yet cleared. */
    List<GeneralLedgerResponse> getUnclearedTransactions(Long id);

    /** Mark a specific GL entry as cleared (or un-cleared) against this reconciliation's bank statement. */
    BankReconciliationResponse toggleTransactionCleared(Long id, Long glEntryId, boolean cleared);

    /** Attaches the actual bank statement file (already uploaded via /api/upload) for audit trail. */
    BankReconciliationResponse attachStatement(Long id, AttachStatementRequest request);

    /**
     * Imports a bank-statement CSV (date, description, amount - positive = deposit,
     * negative = withdrawal; header row auto-detected) and auto-clears uncleared GL
     * transactions whose amount and direction match. Returns matched/unmatched summary.
     */
    StatementImportResult importStatement(Long id, org.springframework.web.multipart.MultipartFile file);

    /** Throws if the adjusted difference (GL vs. bank statement + outstanding items) isn't ~zero. */
    void markAsReconciled(Long id, String notes);

    List<BankReconciliationResponse> getPendingReconciliations();
}
