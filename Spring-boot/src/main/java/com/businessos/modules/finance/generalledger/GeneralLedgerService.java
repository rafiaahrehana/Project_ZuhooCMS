package com.businessos.modules.finance.generalledger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GeneralLedgerService {

    void recordTransaction(Long accountId, BigDecimal debitAmount, BigDecimal creditAmount,
                           String description, GlReferenceType referenceType, Long referenceId,
                           String referenceNumber);

    /**
     * Same as recordTransaction() but with an explicit companyId instead of resolving
     * it from the security context - required by system entry points that have no
     * authenticated user (e.g. the SSLCommerz success/IPN callbacks, which are public
     * endpoints per SecurityConfig). Callers that already have a companyId in hand
     * (the domain entity being posted, or a companyId parameter) should prefer this
     * overload even when a security context does happen to be present.
     */
    void recordTransaction(Long companyId, Long accountId, BigDecimal debitAmount, BigDecimal creditAmount,
                           String description, GlReferenceType referenceType, Long referenceId,
                           String referenceNumber);

    /**
     * Same as the companyId overload but posts dated `transactionDate` instead of always
     * "today" - callers whose source document carries its own business date (an invoice's
     * invoiceDate, an expense's expenseDate, a journal entry's entryDate) should use this
     * one, so the GL entry lands in the accounting period the transaction actually
     * belongs to (and so posting into an already-closed period is rejected - see
     * AccountingPeriodService). Throws BadRequestException if transactionDate falls in a
     * closed period, unless referenceType is YEAR_END_CLOSE (the one entry type allowed
     * to post into the period it's finalizing).
     */
    void recordTransaction(Long companyId, Long accountId, BigDecimal debitAmount, BigDecimal creditAmount,
                           String description, GlReferenceType referenceType, Long referenceId,
                           String referenceNumber, LocalDate transactionDate);

    /**
     * Posts a whole multi-line transaction atomically, rejecting the entire batch up
     * front if sum(debits) != sum(credits) - previously every caller (invoices, expenses,
     * payroll, credit notes...) posted each leg with its own recordTransaction() call and
     * simply trusted itself to pass matching amounts, with nothing anywhere checking that
     * a transaction actually balances before it lands in the ledger. Throws
     * BadRequestException if unbalanced (beyond a one-cent rounding tolerance) or if
     * transactionDate falls in a closed period (same rule as recordTransaction()).
     */
    void recordBalancedTransaction(Long companyId, List<LedgerLine> lines, String description,
                                    GlReferenceType referenceType, Long referenceId, String referenceNumber,
                                    LocalDate transactionDate);

    GeneralLedgerResponse getById(Long id);

    Page<GeneralLedgerResponse> getAll(Pageable pageable);

    Page<GeneralLedgerResponse> getByAccount(Long accountId, Pageable pageable);

    Page<GeneralLedgerResponse> getByDateRange(LocalDate start, LocalDate end, Pageable pageable);

    List<GeneralLedgerResponse> getByReference(GlReferenceType referenceType, Long referenceId);

    void reconcile(Long id, String notes);

    BigDecimal getAccountBalance(Long accountId);
}
