package com.businessos.modules.finance.journalentry;

import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccountRepository;
import com.businessos.modules.finance.generalledger.GeneralLedgerService;
import com.businessos.modules.finance.generalledger.GlReferenceType;
import com.businessos.modules.finance.generalledger.LedgerLine;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalEntryServiceImpl implements JournalEntryService {

    private final JournalEntryRepository jeRepository;
    private final ChartOfAccountRepository coaRepository;
    private final GeneralLedgerService glService;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public JournalEntryResponse create(JournalEntryRequest request) {
        authorizationService.checkPermission(PermissionCode.JOURNAL_ENTRY_CREATE);
        Long companyId = securityUtil.getCurrentCompanyId();

        // Normalize to the multi-line form: an explicit lines list, or two lines
        // synthesized from the legacy single debit/credit fields.
        List<JournalEntryLineRequest> lineRequests = normalizeLines(request);
        validateLines(lineRequests);

        String jeNumber = generateJENumber(companyId);
        JournalEntry je = JournalEntry.builder()
                .companyId(companyId)
                .journalEntryNumber(jeNumber)
                .entryDate(request.getEntryDate() != null ? request.getEntryDate() : LocalDate.now())
                .description(request.getDescription())
                .notes(request.getNotes())
                .createdBy(securityUtil.getCurrentUser().getUsername())
                .createdDate(LocalDate.now())
                .approved(false)
                .posted(false)
                .build();

        BigDecimal totalDebits = BigDecimal.ZERO;
        ChartOfAccount firstDebitAccount = null;
        ChartOfAccount firstCreditAccount = null;

        for (JournalEntryLineRequest lineRequest : lineRequests) {
            ChartOfAccount account = coaRepository.findByIdAndCompanyId(lineRequest.getAccountId(), companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + lineRequest.getAccountId()));
            requireDirectPostingAllowed(account);

            BigDecimal debit = nz(lineRequest.getDebitAmount());
            BigDecimal credit = nz(lineRequest.getCreditAmount());
            totalDebits = totalDebits.add(debit);
            if (firstDebitAccount == null && debit.compareTo(BigDecimal.ZERO) > 0) firstDebitAccount = account;
            if (firstCreditAccount == null && credit.compareTo(BigDecimal.ZERO) > 0) firstCreditAccount = account;

            je.getLines().add(JournalEntryLine.builder()
                    .journalEntry(je)
                    .account(account)
                    .debitAmount(debit)
                    .creditAmount(credit)
                    .lineDescription(lineRequest.getLineDescription())
                    .build());
        }

        // Legacy NOT NULL columns - kept as a summary (see JournalEntry field comment).
        je.setDebitAccount(firstDebitAccount);
        je.setCreditAccount(firstCreditAccount);
        je.setAmount(totalDebits);

        je = jeRepository.save(je);
        return JournalEntryMapper.toResponse(je);
    }

    /**
     * The UI shows a "NO DIRECT POSTING" badge and explains isHeaderAccount /
     * !allowDirectPosting as a real restriction, but nothing server-side ever
     * enforced it - any user could post a manual journal entry directly to an
     * account meant as a pure rollup, silently breaking its hierarchy. System-
     * generated postings (invoices, expenses, payroll) always resolve to
     * specific leaf accounts via DefaultAccountResolver, never a header
     * account, so this only needs to guard the one place a human picks an
     * account by hand.
     */
    private void requireDirectPostingAllowed(ChartOfAccount account) {
        if (account.isHeaderAccount() || !account.isAllowDirectPosting()) {
            throw new BadRequestException(
                    "\"" + account.getAccountName() + "\" (" + account.getAccountCode()
                            + ") does not allow direct posting - it's a header/rollup account. Post to one of its child accounts instead.");
        }
    }

    /** Either the explicit lines list, or two lines built from the legacy 1:1 fields. */
    private List<JournalEntryLineRequest> normalizeLines(JournalEntryRequest request) {
        if (request.getLines() != null && !request.getLines().isEmpty()) {
            return request.getLines();
        }
        if (request.getDebitAccountId() == null || request.getCreditAccountId() == null || request.getAmount() == null) {
            throw new BadRequestException(
                    "Provide either a lines list (at least 2 lines) or the legacy debitAccountId/creditAccountId/amount fields");
        }
        return List.of(
                JournalEntryLineRequest.builder()
                        .accountId(request.getDebitAccountId())
                        .debitAmount(request.getAmount())
                        .creditAmount(BigDecimal.ZERO)
                        .build(),
                JournalEntryLineRequest.builder()
                        .accountId(request.getCreditAccountId())
                        .debitAmount(BigDecimal.ZERO)
                        .creditAmount(request.getAmount())
                        .build());
    }

    private void validateLines(List<JournalEntryLineRequest> lines) {
        if (lines.size() < 2) {
            throw new BadRequestException("A journal entry needs at least 2 lines");
        }
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (JournalEntryLineRequest line : lines) {
            BigDecimal debit = nz(line.getDebitAmount());
            BigDecimal credit = nz(line.getCreditAmount());
            if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Line amounts cannot be negative");
            }
            if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
                throw new BadRequestException("A line can be a debit or a credit, not both");
            }
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                throw new BadRequestException("Every line needs a debit or credit amount");
            }
            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);
        }
        if (totalDebits.subtract(totalCredits).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BadRequestException("Journal entry does not balance: debits " + totalDebits
                    + " vs credits " + totalCredits);
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntryResponse getById(Long id) {
        authorizationService.checkPermission(PermissionCode.JOURNAL_ENTRY_VIEW);
        return JournalEntryMapper.toResponse(findInTenant(id));
    }

    private JournalEntry findInTenant(Long id) {
        return jeRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JournalEntryResponse> getAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.JOURNAL_ENTRY_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return jeRepository.findByCompanyId(companyId, pageable)
                .map(JournalEntryMapper::toResponse);
    }

    @Override
    @Transactional
    public void approve(Long id) {
        authorizationService.checkPermission(PermissionCode.JOURNAL_ENTRY_APPROVE);
        JournalEntry je = findInTenant(id);

        if (je.isApproved()) {
            throw new BadRequestException("Journal entry is already approved");
        }

        // Maker-checker: the person who wrote an entry must not be the one who approves
        // it - otherwise the approval step is theater and one compromised/mistaken user
        // can move money through the books alone.
        String approver = securityUtil.getCurrentUser().getUsername();
        if (approver != null && approver.equalsIgnoreCase(je.getCreatedBy())) {
            throw new BadRequestException("You created this journal entry - a different user must approve it");
        }

        je.approve(approver);
        jeRepository.save(je);
    }

    @Override
    @Transactional
    public void post(Long id) {
        authorizationService.checkPermission(PermissionCode.JOURNAL_ENTRY_POST);
        JournalEntry je = findInTenant(id);

        if (!je.isApproved()) {
            throw new BadRequestException("Journal entry must be approved before posting");
        }

        if (je.isPosted()) {
            throw new BadRequestException("Journal entry is already posted");
        }

        postToLedger(je);
        jeRepository.save(je);
    }

    /**
     * Records the entry's GL lines as one balanced batch (rejected up front if debits
     * don't equal credits) and flips it to posted. Shared by {@link #post} and
     * {@link #reverse}. Pre-lines entries fall back to the legacy 1:1 columns.
     */
    private void postToLedger(JournalEntry je) {
        LocalDate transactionDate = je.getEntryDate() != null ? je.getEntryDate() : LocalDate.now();

        List<LedgerLine> ledgerLines;
        if (je.getLines() != null && !je.getLines().isEmpty()) {
            ledgerLines = je.getLines().stream()
                    .map(line -> new LedgerLine(line.getAccount().getId(),
                            nz(line.getDebitAmount()), nz(line.getCreditAmount())))
                    .collect(java.util.stream.Collectors.toList());
        } else {
            ledgerLines = List.of(
                    LedgerLine.debit(je.getDebitAccount().getId(), je.getAmount()),
                    LedgerLine.credit(je.getCreditAccount().getId(), je.getAmount()));
        }

        glService.recordBalancedTransaction(je.getCompanyId(), ledgerLines, je.getDescription(),
                GlReferenceType.JOURNAL_ENTRY, je.getId(), je.getJournalEntryNumber(), transactionDate);

        je.post();
    }

    @Override
    @Transactional
    public JournalEntryResponse reverse(Long id) {
        authorizationService.checkPermission(PermissionCode.JOURNAL_ENTRY_POST);
        JournalEntry original = findInTenant(id);

        if (!original.isPosted()) {
            throw new BadRequestException("Only posted journal entries can be reversed");
        }
        if (original.isReversed()) {
            throw new BadRequestException("Journal entry is already reversed");
        }

        Long companyId = original.getCompanyId();
        String actor = securityUtil.getCurrentUser().getUsername();
        LocalDate today = LocalDate.now();

        // Reversing entry mirrors the original with debit and credit swapped, so
        // posting it produces the exact offsetting ledger movement. It's created
        // pre-approved and posted immediately.
        JournalEntry reversal = JournalEntry.builder()
                .companyId(companyId)
                .journalEntryNumber(generateJENumber(companyId))
                .entryDate(today)
                .debitAccount(original.getCreditAccount())
                .creditAccount(original.getDebitAccount())
                .amount(original.getAmount())
                .description("Reversal of " + original.getJournalEntryNumber()
                        + (original.getDescription() != null ? " — " + original.getDescription() : ""))
                .notes("Auto-generated reversal")
                .createdBy(actor)
                .createdDate(today)
                .reversedFromEntryId(original.getId())
                .approved(true)
                .approvedBy(actor)
                .approvedDate(today)
                .posted(false)
                .build();

        // Mirror every line of the original with debit/credit swapped - a multi-line
        // original needs a matching multi-line reversal, not just the summary pair.
        if (original.getLines() != null && !original.getLines().isEmpty()) {
            for (JournalEntryLine line : original.getLines()) {
                reversal.getLines().add(JournalEntryLine.builder()
                        .journalEntry(reversal)
                        .account(line.getAccount())
                        .debitAmount(nz(line.getCreditAmount()))
                        .creditAmount(nz(line.getDebitAmount()))
                        .lineDescription(line.getLineDescription())
                        .build());
            }
        }

        reversal = jeRepository.save(reversal);

        postToLedger(reversal);
        reversal = jeRepository.save(reversal);

        original.markReversed(reversal.getId());
        jeRepository.save(original);

        return JournalEntryMapper.toResponse(reversal);
    }

    @Override
    @Transactional
    public JournalEntryResponse delete(Long id) {
        authorizationService.checkPermission(PermissionCode.JOURNAL_ENTRY_DELETE);
        JournalEntry je = findInTenant(id);

        if (je.isPosted()) {
            throw new BadRequestException("Cannot delete posted journal entries");
        }

        je.softDelete();
        jeRepository.save(je);
        return JournalEntryMapper.toResponse(je);
    }

    private String generateJENumber(Long companyId) {
        int year = LocalDate.now().getYear();
        String prefix = "JE-" + year + "-";
        String maxNumber = jeRepository
                .findMaxJENumberByCompanyAndPrefix(companyId, prefix)
                .orElse(prefix + "000000");
        long sequence = Long.parseLong(maxNumber.substring(prefix.length())) + 1;
        return String.format("%s%06d", prefix, sequence);
    }
}

