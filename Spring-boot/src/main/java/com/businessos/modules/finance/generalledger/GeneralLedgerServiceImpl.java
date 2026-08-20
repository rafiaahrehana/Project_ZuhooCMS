package com.businessos.modules.finance.generalledger;

import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccountRepository;
import com.businessos.modules.finance.period.PeriodLockChecker;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeneralLedgerServiceImpl implements GeneralLedgerService {

    private final GeneralLedgerRepository glRepository;
    private final ChartOfAccountRepository coaRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final PeriodLockChecker periodLockChecker;

    @Override
    @Transactional
    public void recordTransaction(Long accountId, BigDecimal debitAmount, BigDecimal creditAmount,
                                  String description, GlReferenceType referenceType, Long referenceId,
                                  String referenceNumber) {
        recordTransaction(securityUtil.getCurrentCompanyId(), accountId, debitAmount, creditAmount,
                description, referenceType, referenceId, referenceNumber, LocalDate.now());
    }

    @Override
    @Transactional
    public void recordTransaction(Long companyId, Long accountId, BigDecimal debitAmount, BigDecimal creditAmount,
                                  String description, GlReferenceType referenceType, Long referenceId,
                                  String referenceNumber) {
        recordTransaction(companyId, accountId, debitAmount, creditAmount,
                description, referenceType, referenceId, referenceNumber, LocalDate.now());
    }

    @Override
    @Transactional
    public void recordTransaction(Long companyId, Long accountId, BigDecimal debitAmount, BigDecimal creditAmount,
                                  String description, GlReferenceType referenceType, Long referenceId,
                                  String referenceNumber, LocalDate transactionDate) {
        LocalDate date = transactionDate != null ? transactionDate : LocalDate.now();

        // The year-end close is the one entry type allowed to post into the period it's
        // finalizing - every other poster is blocked from backdating into a closed period.
        if (referenceType != GlReferenceType.YEAR_END_CLOSE && periodLockChecker.isDateInClosedPeriod(companyId, date)) {
            throw new BadRequestException(
                    "Cannot post to " + date + " - that accounting period is closed. Reopen it first if this entry truly belongs there.");
        }

        ChartOfAccount account = coaRepository.findByIdAndCompanyId(accountId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Chart of Account not found"));

        var currentUser = securityUtil.getCurrentUser();
        GeneralLedger entry = GeneralLedger.builder()
                .companyId(companyId)
                .transactionDate(date)
                .account(account)
                .debitAmount(debitAmount != null ? debitAmount : BigDecimal.ZERO)
                .creditAmount(creditAmount != null ? creditAmount : BigDecimal.ZERO)
                .description(description)
                .referenceType(referenceType.name())
                .referenceId(referenceId)
                .referenceNumber(referenceNumber)
                .posted(true)
                // No authenticated user for system entry points (e.g. payment gateway callbacks).
                .postedBy(currentUser != null ? currentUser.getUsername() : "System")
                .postedDate(LocalDate.now())
                .build();

        glRepository.save(entry);

        // Update account balance
        updateAccountBalance(account, debitAmount, creditAmount);
    }

    @Override
    @Transactional
    public void recordBalancedTransaction(Long companyId, List<LedgerLine> lines, String description,
                                           GlReferenceType referenceType, Long referenceId, String referenceNumber,
                                           LocalDate transactionDate) {
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("A transaction needs at least one line");
        }

        BigDecimal totalDebits = lines.stream()
                .map(l -> l.debitAmount() != null ? l.debitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = lines.stream()
                .map(l -> l.creditAmount() != null ? l.creditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.subtract(totalCredits).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BadRequestException("Transaction does not balance: debits " + totalDebits
                    + " vs credits " + totalCredits + " - rejected before posting anything");
        }

        for (LedgerLine line : lines) {
            boolean bothZero = isZero(line.debitAmount()) && isZero(line.creditAmount());
            if (bothZero) continue; // a genuinely empty line is a no-op, not an error
            recordTransaction(companyId, line.accountId(), line.debitAmount(), line.creditAmount(),
                    description, referenceType, referenceId, referenceNumber, transactionDate);
        }
    }

    private static boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    @Transactional(readOnly = true)
    public GeneralLedgerResponse getById(Long id) {
        authorizationService.checkPermission(PermissionCode.GENERAL_LEDGER_VIEW);
        GeneralLedger entry = glRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("GL entry not found"));
        return GeneralLedgerMapper.toResponse(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GeneralLedgerResponse> getAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.GENERAL_LEDGER_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return glRepository.findByCompanyId(companyId, pageable)
                .map(GeneralLedgerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GeneralLedgerResponse> getByAccount(Long accountId, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.GENERAL_LEDGER_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return glRepository.findByCompanyIdAndAccountId(companyId, accountId, pageable)
                .map(GeneralLedgerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GeneralLedgerResponse> getByDateRange(LocalDate start, LocalDate end, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.GENERAL_LEDGER_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return glRepository.findByCompanyIdAndTransactionDateBetween(companyId, start, end, pageable)
                .map(GeneralLedgerMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneralLedgerResponse> getByReference(GlReferenceType referenceType, Long referenceId) {
        authorizationService.checkPermission(PermissionCode.GENERAL_LEDGER_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return glRepository.findByCompanyIdAndReferenceTypeAndReferenceId(companyId, referenceType.name(), referenceId)
                .stream()
                .map(GeneralLedgerMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reconcile(Long id, String notes) {
        authorizationService.checkPermission(PermissionCode.GENERAL_LEDGER_RECONCILE);
        GeneralLedger entry = glRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("GL entry not found"));
        entry.setReconciled(true);
        entry.setReconciliationNotes(notes);
        glRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId) {
        ChartOfAccount account = coaRepository.findByIdAndCompanyId(accountId, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        return account.getBalance();
    }

    /**
     * Debit-normal accounts (ASSET, CONTRA_LIABILITY, EXPENSE, CONTRA_REVENUE) increase
     * with a debit and decrease with a credit. Credit-normal accounts (LIABILITY,
     * CONTRA_ASSET, EQUITY, REVENUE) are the opposite - a credit increases them.
     * Classification lives on AccountType.isCreditNormal() - the single source of truth.
     */
    private void updateAccountBalance(ChartOfAccount account, BigDecimal debitAmount, BigDecimal creditAmount) {
        BigDecimal currentBalance = account.getBalance();
        boolean isCreditNormalType = account.getType().isCreditNormal();

        BigDecimal debit = debitAmount != null ? debitAmount : BigDecimal.ZERO;
        BigDecimal credit = creditAmount != null ? creditAmount : BigDecimal.ZERO;

        if (isCreditNormalType) {
            currentBalance = currentBalance.add(credit).subtract(debit);
        } else {
            currentBalance = currentBalance.add(debit).subtract(credit);
        }

        account.setBalance(currentBalance);
        coaRepository.save(account);
    }
}
