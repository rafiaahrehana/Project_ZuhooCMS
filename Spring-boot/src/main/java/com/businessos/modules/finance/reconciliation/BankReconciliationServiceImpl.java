package com.businessos.modules.finance.reconciliation;

import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccountRepository;
import com.businessos.modules.finance.generalledger.GeneralLedger;
import com.businessos.modules.finance.generalledger.GeneralLedgerMapper;
import com.businessos.modules.finance.generalledger.GeneralLedgerRepository;
import com.businessos.modules.finance.generalledger.GeneralLedgerResponse;
import com.businessos.modules.finance.generalledger.GeneralLedgerService;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BankReconciliationServiceImpl implements BankReconciliationService {

    // Reconciling systems always compare rounded currency amounts - anything smaller
    // than a cent is float/rounding noise, not a real discrepancy.
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final BankReconciliationRepository reconciliationRepository;
    private final ChartOfAccountRepository coaRepository;
    private final GeneralLedgerService glService;
    private final GeneralLedgerRepository glRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public BankReconciliationResponse create(BankReconciliationRequest request) {
        authorizationService.checkPermission(PermissionCode.BANK_RECONCILIATION_CREATE);
        Long companyId = securityUtil.getCurrentCompanyId();

        ChartOfAccount account = coaRepository.findByIdAndCompanyId(request.getBankAccountId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found"));

        BigDecimal glBalance = glService.getAccountBalance(account.getId());

        BankReconciliation reconciliation = BankReconciliation.builder()
                .companyId(companyId)
                .bankAccount(account)
                .reconciliationDate(LocalDate.now())
                .glBalance(glBalance)
                .bankStatementBalance(request.getBankStatementBalance())
                .reconciled(false)
                .build();

        recompute(reconciliation);
        reconciliation = reconciliationRepository.save(reconciliation);
        return BankReconciliationMapper.toResponse(reconciliation);
    }

    @Override
    @Transactional(readOnly = true)
    public BankReconciliationResponse getById(Long id) {
        authorizationService.checkPermission(PermissionCode.BANK_RECONCILIATION_VIEW);
        return BankReconciliationMapper.toResponse(findInTenant(id));
    }

    private BankReconciliation findInTenant(Long id) {
        return reconciliationRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BankReconciliationResponse> getAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.BANK_RECONCILIATION_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return reconciliationRepository.findByCompanyId(companyId, pageable)
                .map(BankReconciliationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneralLedgerResponse> getUnclearedTransactions(Long id) {
        authorizationService.checkPermission(PermissionCode.BANK_RECONCILIATION_VIEW);
        BankReconciliation reconciliation = findInTenant(id);
        return uncleared(reconciliation).stream()
                .map(GeneralLedgerMapper::toResponse)
                .collect(Collectors.toList());
    }

    private List<GeneralLedger> uncleared(BankReconciliation reconciliation) {
        return glRepository.findByCompanyIdAndAccountIdAndIsReconciledFalseAndTransactionDateLessThanEqualOrderByTransactionDateAsc(
                reconciliation.getCompanyId(), reconciliation.getBankAccount().getId(), reconciliation.getReconciliationDate());
    }

    @Override
    @Transactional
    public BankReconciliationResponse toggleTransactionCleared(Long id, Long glEntryId, boolean cleared) {
        authorizationService.checkPermission(PermissionCode.BANK_RECONCILIATION_RECONCILE);
        BankReconciliation reconciliation = findInTenant(id);
        if (reconciliation.isReconciled()) {
            throw new BadRequestException("This reconciliation is already closed - reopen it isn't supported, start a new one instead");
        }

        Long reconciliationCompanyId = reconciliation.getCompanyId();
        Long reconciliationAccountId = reconciliation.getBankAccount().getId();
        GeneralLedger entry = glRepository.findById(glEntryId)
                .filter(e -> Objects.equals(e.getCompanyId(), reconciliationCompanyId))
                .filter(e -> Objects.equals(e.getAccount().getId(), reconciliationAccountId))
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found on this account: " + glEntryId));

        if (cleared) {
            entry.setReconciled(true);
            entry.setReconciledInReconciliationId(reconciliation.getId());
        } else {
            // Only let this reconciliation un-clear a line it cleared itself - otherwise
            // one reconciliation could silently reopen a different, already-closed one.
            if (!Objects.equals(entry.getReconciledInReconciliationId(), reconciliation.getId())) {
                throw new BadRequestException("This transaction wasn't cleared by this reconciliation");
            }
            entry.setReconciled(false);
            entry.setReconciledInReconciliationId(null);
        }
        glRepository.save(entry);

        recompute(reconciliation);
        reconciliation = reconciliationRepository.save(reconciliation);
        return BankReconciliationMapper.toResponse(reconciliation);
    }

    /**
     * adjustedBankBalance = bankStatementBalance + outstanding deposits (debits still
     * uncleared) - outstanding checks (credits still uncleared). difference is what's
     * left unexplained between the books and the bank once every cleared transaction
     * is accounted for - markAsReconciled() requires this to be ~zero.
     */
    private void recompute(BankReconciliation reconciliation) {
        List<GeneralLedger> outstanding = uncleared(reconciliation);
        BigDecimal deposits = outstanding.stream()
                .map(GeneralLedger::getDebitAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal checks = outstanding.stream()
                .map(GeneralLedger::getCreditAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal bankStatementBalance = reconciliation.getBankStatementBalance() != null
                ? reconciliation.getBankStatementBalance() : BigDecimal.ZERO;
        BigDecimal adjustedBankBalance = bankStatementBalance.add(deposits).subtract(checks);
        // Re-read live rather than reusing the value snapshotted at session
        // creation: a GL entry posted (or backdated) into this account after
        // the session opened moves the true balance, and reusing the stale
        // snapshot here made the difference permanently unresolvable even
        // when the books and bank statement actually agreed.
        BigDecimal glBalance = glService.getAccountBalance(reconciliation.getBankAccount().getId());
        reconciliation.setGlBalance(glBalance);

        reconciliation.setOutstandingDepositsTotal(deposits);
        reconciliation.setOutstandingChecksTotal(checks);
        reconciliation.setDifference(glBalance.subtract(adjustedBankBalance));
    }

    @Override
    @Transactional
    public BankReconciliationResponse attachStatement(Long id, AttachStatementRequest request) {
        authorizationService.checkPermission(PermissionCode.BANK_RECONCILIATION_RECONCILE);
        BankReconciliation reconciliation = findInTenant(id);
        reconciliation.setStatementFileName(request.getFileName());
        reconciliation.setStatementFileUrl(request.getFileUrl());
        reconciliation.setStatementUploadedAt(java.time.LocalDateTime.now());
        reconciliation = reconciliationRepository.save(reconciliation);
        return BankReconciliationMapper.toResponse(reconciliation);
    }

    @Override
    @Transactional
    public StatementImportResult importStatement(Long id, org.springframework.web.multipart.MultipartFile file) {
        authorizationService.checkPermission(PermissionCode.BANK_RECONCILIATION_RECONCILE);
        BankReconciliation reconciliation = findInTenant(id);
        if (reconciliation.isReconciled()) {
            throw new BadRequestException("This reconciliation is already closed");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file uploaded");
        }

        String content;
        try {
            content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new BadRequestException("Could not read the uploaded file");
        }

        List<GeneralLedger> candidates = new java.util.ArrayList<>(uncleared(reconciliation));
        List<StatementImportResult.UnmatchedLine> unmatched = new java.util.ArrayList<>();
        int totalLines = 0;
        int matched = 0;

        for (String rawLine : content.split("\r?\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            String[] cols = line.split(line.contains(";") ? ";" : ",", -1);
            if (cols.length < 2) continue;
            String amountText = cols[cols.length - 1].replace("\"", "").replace(",", "").trim();

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountText);
            } catch (NumberFormatException e) {
                // Non-numeric last column: a header row ("date,description,amount") - skip silently.
                continue;
            }

            totalLines++;
            String date = cols[0].replace("\"", "").trim();
            String description = cols.length >= 3
                    ? String.join(",", java.util.Arrays.asList(cols).subList(1, cols.length - 1)).replace("\"", "").trim()
                    : "";

            // Positive statement amount = money into the bank = a debit on the bank's GL
            // account; negative = money out = a credit. Match the first uncleared entry
            // with the right direction and the same amount (one statement line consumes
            // at most one GL entry).
            boolean deposit = amount.compareTo(BigDecimal.ZERO) >= 0;
            BigDecimal absAmount = amount.abs();
            GeneralLedger match = null;
            for (GeneralLedger entry : candidates) {
                BigDecimal entryAmount = deposit ? entry.getDebitAmount() : entry.getCreditAmount();
                if (entryAmount != null && entryAmount.compareTo(BigDecimal.ZERO) > 0
                        && entryAmount.subtract(absAmount).abs().compareTo(TOLERANCE) <= 0) {
                    match = entry;
                    break;
                }
            }

            if (match != null) {
                match.setReconciled(true);
                match.setReconciledInReconciliationId(reconciliation.getId());
                match.setReconciliationNotes("Auto-matched from statement import"
                        + (description.isEmpty() ? "" : ": " + description));
                glRepository.save(match);
                candidates.remove(match);
                matched++;
            } else {
                unmatched.add(StatementImportResult.UnmatchedLine.builder()
                        .date(date)
                        .description(description)
                        .amount(amount)
                        .reason("No uncleared " + (deposit ? "deposit" : "withdrawal") + " of this amount in the books")
                        .build());
            }
        }

        if (totalLines == 0) {
            throw new BadRequestException(
                    "No transaction rows found - expected CSV columns: date, description, amount (negative = withdrawal)");
        }

        recompute(reconciliation);
        reconciliation = reconciliationRepository.save(reconciliation);

        return StatementImportResult.builder()
                .totalLines(totalLines)
                .matched(matched)
                .unmatchedCount(unmatched.size())
                .unmatchedLines(unmatched)
                .reconciliation(BankReconciliationMapper.toResponse(reconciliation))
                .build();
    }

    @Override
    @Transactional
    public void markAsReconciled(Long id, String notes) {
        authorizationService.checkPermission(PermissionCode.BANK_RECONCILIATION_RECONCILE);
        BankReconciliation reconciliation = findInTenant(id);
        if (reconciliation.isReconciled()) {
            throw new BadRequestException("Already reconciled");
        }

        // Recompute fresh rather than trusting whatever was last saved - a GL entry
        // could have posted since this reconciliation was opened.
        recompute(reconciliation);
        if (reconciliation.getDifference().abs().compareTo(TOLERANCE) > 0) {
            throw new BadRequestException(
                    "Cannot close this reconciliation - the books and the bank statement still differ by "
                            + reconciliation.getDifference().abs()
                            + " after accounting for outstanding items. Clear more transactions against the "
                            + "bank statement, or post a journal entry for any bank fee/interest not yet recorded, then try again.");
        }

        reconciliation.markAsReconciled(securityUtil.getCurrentUser().getUsername());
        reconciliation.setDiscrepancyNotes(notes);
        reconciliationRepository.save(reconciliation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankReconciliationResponse> getPendingReconciliations() {
        authorizationService.checkPermission(PermissionCode.BANK_RECONCILIATION_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        // Map to the response DTO while the transaction (and Hibernate session) is
        // still open - bankAccount is FetchType.LAZY, so mapping this after the
        // service method returns (as the controller previously did) throws
        // LazyInitializationException the moment the mapper touches getBankAccount().
        return reconciliationRepository.findByCompanyIdAndReconciledFalse(companyId)
                .stream()
                .map(BankReconciliationMapper::toResponse)
                .collect(Collectors.toList());
    }
}
