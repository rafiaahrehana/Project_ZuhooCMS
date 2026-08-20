package com.businessos.modules.finance.period;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.finance.chartofaccounts.AccountType;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccountRepository;
import com.businessos.modules.finance.chartofaccounts.DefaultAccountResolver;
import com.businessos.modules.finance.generalledger.GeneralLedger;
import com.businessos.modules.finance.generalledger.GeneralLedgerRepository;
import com.businessos.modules.finance.generalledger.GeneralLedgerService;
import com.businessos.modules.finance.generalledger.GlReferenceType;
import com.businessos.modules.finance.generalledger.LedgerLine;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountingPeriodServiceImpl implements AccountingPeriodService {

    private final AccountingPeriodRepository periodRepository;
    private final CompanyRepository companyRepository;
    private final ChartOfAccountRepository coaRepository;
    private final GeneralLedgerRepository glRepository;
    private final GeneralLedgerService glService;
    private final DefaultAccountResolver accountResolver;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;
    private final PeriodLockChecker periodLockChecker;

    @Override
    @Transactional
    public List<AccountingPeriodResponse> listForYear(int fiscalYear) {
        authorizationService.checkPermission(PermissionCode.ACCOUNTING_PERIOD_VIEW);
        Long companyId = requireCompanyId();
        List<AccountingPeriod> periods = ensureYearExists(companyId, fiscalYear);
        return periods.stream().map(AccountingPeriodMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Generates any of the 12 monthly periods for this fiscal year that don't exist yet,
     * from the company's fiscalYearStartMonth. Convention: fiscal year Y's period 1 starts
     * on (fiscalYearStartMonth, Y) and runs 12 months forward - e.g. a July-start company's
     * "FY2026" is Jul 2026 - Jun 2027. (Some real companies instead name a July-start year
     * by its END year; this app picks the start-year convention consistently everywhere.)
     */
    private List<AccountingPeriod> ensureYearExists(Long companyId, int fiscalYear) {
        List<AccountingPeriod> existing = periodRepository.findByCompanyIdAndFiscalYearOrderByPeriodNumberAsc(companyId, fiscalYear);
        Map<Integer, AccountingPeriod> byNumber = new HashMap<>();
        existing.forEach(p -> byNumber.put(p.getPeriodNumber(), p));
        if (byNumber.size() == 12) return existing;

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        int startMonth = company.getFiscalYearStartMonth() != null ? company.getFiscalYearStartMonth() : 1;

        List<AccountingPeriod> result = new ArrayList<>();
        LocalDate cursor = LocalDate.of(fiscalYear, startMonth, 1);
        for (int i = 1; i <= 12; i++) {
            AccountingPeriod period = byNumber.get(i);
            if (period == null) {
                LocalDate end = cursor.plusMonths(1).minusDays(1);
                period = periodRepository.save(AccountingPeriod.builder()
                        .companyId(companyId)
                        .fiscalYear(fiscalYear)
                        .periodNumber(i)
                        .startDate(cursor)
                        .endDate(end)
                        .status(PeriodStatus.OPEN)
                        .build());
            }
            result.add(period);
            cursor = cursor.plusMonths(1);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiscalYearSummary> listFiscalYears() {
        authorizationService.checkPermission(PermissionCode.ACCOUNTING_PERIOD_VIEW);
        Long companyId = requireCompanyId();
        LocalDate today = LocalDate.now();

        Map<Integer, List<AccountingPeriod>> byYear = periodRepository
                .findByCompanyIdOrderByFiscalYearAscPeriodNumberAsc(companyId)
                .stream()
                .collect(Collectors.groupingBy(AccountingPeriod::getFiscalYear,
                        java.util.LinkedHashMap::new, Collectors.toList()));

        List<FiscalYearSummary> summaries = new ArrayList<>();
        for (Map.Entry<Integer, List<AccountingPeriod>> entry : byYear.entrySet()) {
            int year = entry.getKey();
            List<AccountingPeriod> periods = entry.getValue();
            LocalDate start = periods.get(0).getStartDate();
            LocalDate end = periods.get(periods.size() - 1).getEndDate();
            int closed = (int) periods.stream().filter(p -> p.getStatus() == PeriodStatus.CLOSED).count();

            boolean yearEndPosted = !glRepository.findByCompanyIdAndReferenceTypeAndReferenceId(
                    companyId, GlReferenceType.YEAR_END_CLOSE.name(), (long) year).isEmpty();
            boolean current = !today.isBefore(start) && !today.isAfter(end);

            String status;
            if (yearEndPosted) status = "CLOSED";
            else if (current || closed > 0) status = "ACTIVE";
            else status = "DRAFT";

            summaries.add(FiscalYearSummary.builder()
                    .fiscalYear(year)
                    .name("FY " + year)
                    .startDate(start)
                    .endDate(end)
                    .totalPeriods(periods.size())
                    .openPeriods(periods.size() - closed)
                    .closedPeriods(closed)
                    .status(status)
                    .yearEndPosted(yearEndPosted)
                    .current(current)
                    .createdAt(periods.get(0).getCreatedAt())
                    .build());
        }
        return summaries;
    }

    @Override
    @Transactional
    public AccountingPeriodResponse closePeriod(Long id) {
        authorizationService.checkPermission(PermissionCode.ACCOUNTING_PERIOD_CLOSE);
        Long companyId = requireCompanyId();
        AccountingPeriod period = findInTenant(id, companyId);

        if (period.getStatus() == PeriodStatus.CLOSED) {
            throw new BadRequestException("This period is already closed");
        }

        // Close strictly in date order - otherwise a later period could close while an
        // earlier one is still open, letting someone backdate into the still-open gap
        // and produce financials that contradict a period already reported as final.
        LocalDate periodStart = period.getStartDate();
        boolean earlierStillOpen = periodRepository.findByCompanyIdAndFiscalYearOrderByPeriodNumberAsc(companyId, period.getFiscalYear())
                .stream()
                .anyMatch(p -> p.getStartDate().isBefore(periodStart) && p.getStatus() == PeriodStatus.OPEN);
        if (earlierStillOpen) {
            throw new BadRequestException("Close earlier periods first - periods must be closed in date order");
        }

        period.setStatus(PeriodStatus.CLOSED);
        period.setClosedBy(securityUtil.getCurrentUser().getUsername());
        period.setClosedAt(java.time.LocalDateTime.now());
        period = periodRepository.save(period);
        return AccountingPeriodMapper.toResponse(period);
    }

    @Override
    @Transactional
    public AccountingPeriodResponse reopenPeriod(Long id) {
        authorizationService.checkPermission(PermissionCode.ACCOUNTING_PERIOD_CLOSE);
        Long companyId = requireCompanyId();
        AccountingPeriod period = findInTenant(id, companyId);

        if (period.getStatus() != PeriodStatus.CLOSED) {
            throw new BadRequestException("This period isn't closed");
        }
        // A closed fiscal year already posted its YEAR_END_CLOSE entry, computed
        // from exactly the GL movement its 12 periods had at that moment.
        // Reopening one of those periods and posting into it would silently
        // invalidate that already-posted closing entry with no way to
        // regenerate it (closeFiscalYear refuses to run twice) - block it here
        // instead, same check closeFiscalYear itself uses.
        if (isFiscalYearClosed(companyId, period.getFiscalYear())) {
            throw new BadRequestException(
                    "Fiscal year " + period.getFiscalYear() + " has already been closed - its periods can no longer be reopened");
        }
        // Reopen strictly in reverse date order, mirroring the close constraint - otherwise
        // you'd have an open period sitting before a still-closed later one.
        LocalDate periodStart = period.getStartDate();
        boolean laterAlreadyOpen = periodRepository.findByCompanyIdAndFiscalYearOrderByPeriodNumberAsc(companyId, period.getFiscalYear())
                .stream()
                .anyMatch(p -> p.getStartDate().isAfter(periodStart) && p.getStatus() == PeriodStatus.OPEN);
        if (laterAlreadyOpen) {
            throw new BadRequestException("Reopen later periods first - periods must be reopened in reverse date order");
        }

        period.setStatus(PeriodStatus.OPEN);
        period.setReopenedBy(securityUtil.getCurrentUser().getUsername());
        period.setReopenedAt(java.time.LocalDateTime.now());
        period = periodRepository.save(period);
        return AccountingPeriodMapper.toResponse(period);
    }

    @Override
    @Transactional
    public void closeFiscalYear(int fiscalYear) {
        authorizationService.checkPermission(PermissionCode.ACCOUNTING_PERIOD_CLOSE);
        Long companyId = requireCompanyId();

        List<AccountingPeriod> periods = periodRepository.findByCompanyIdAndFiscalYearOrderByPeriodNumberAsc(companyId, fiscalYear);
        if (periods.size() < 12) {
            throw new BadRequestException("Not all 12 periods exist yet for fiscal year " + fiscalYear);
        }
        boolean allClosed = periods.stream().allMatch(p -> p.getStatus() == PeriodStatus.CLOSED);
        if (!allClosed) {
            throw new BadRequestException("All 12 periods must be closed before closing fiscal year " + fiscalYear);
        }

        if (isFiscalYearClosed(companyId, fiscalYear)) {
            throw new BadRequestException("Fiscal year " + fiscalYear + " has already been closed");
        }

        LocalDate start = periods.get(0).getStartDate();
        LocalDate end = periods.get(periods.size() - 1).getEndDate();
        String description = "Fiscal year " + fiscalYear + " (" + start + " to " + end + ") closing entry";

        // Zero out every Revenue/Contra-Revenue/Expense account's movement for the year,
        // routing the net into Retained Earnings - the same "close the books" entry real
        // bookkeeping makes so next year's P&L starts from zero, not last year's total.
        // Collected into one line list and posted as a single recordBalancedTransaction()
        // call, so the whole closing entry either lands atomically and balanced, or not
        // at all - rather than trusting dozens of individually-balanced pairs to add up.
        List<LedgerLine> lines = new ArrayList<>();
        BigDecimal netIncome = BigDecimal.ZERO;
        netIncome = netIncome.add(closeAccountsOfType(companyId, AccountType.REVENUE, start, end, lines));
        netIncome = netIncome.subtract(closeAccountsOfType(companyId, AccountType.CONTRA_REVENUE, start, end, lines));
        netIncome = netIncome.subtract(closeAccountsOfType(companyId, AccountType.EXPENSE, start, end, lines));

        if (netIncome.compareTo(BigDecimal.ZERO) == 0) {
            return; // nothing moved this year - no closing entry needed
        }

        ChartOfAccount retainedEarnings = accountResolver.retainedEarnings(companyId);
        if (netIncome.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(LedgerLine.credit(retainedEarnings.getId(), netIncome));
        } else {
            lines.add(LedgerLine.debit(retainedEarnings.getId(), netIncome.negate()));
        }

        glService.recordBalancedTransaction(companyId, lines, description,
                GlReferenceType.YEAR_END_CLOSE, (long) fiscalYear, "FY" + fiscalYear, end);
    }

    private boolean isFiscalYearClosed(Long companyId, int fiscalYear) {
        return !glRepository
                .findByCompanyIdAndReferenceTypeAndReferenceId(companyId, GlReferenceType.YEAR_END_CLOSE.name(), (long) fiscalYear)
                .isEmpty();
    }

    /**
     * Appends the zeroing line(s) for every account of one type to `lines` and returns the
     * total "normal-direction" movement closed out (positive = that type had a normal
     * credit-normal-or-debit-normal balance for the year, as appropriate to its type).
     */
    private BigDecimal closeAccountsOfType(Long companyId, AccountType type, LocalDate start, LocalDate end,
                                            List<LedgerLine> lines) {
        boolean creditNormal = type.isCreditNormal();
        BigDecimal total = BigDecimal.ZERO;

        for (ChartOfAccount account : coaRepository.findByCompanyIdAndType(companyId, type)) {
            List<GeneralLedger> entries = glRepository.findByCompanyIdAndAccountIdAndTransactionDateBetween(
                    companyId, account.getId(), start, end);
            BigDecimal movement = entries.stream()
                    .map(gl -> creditNormal
                            ? nz(gl.getCreditAmount()).subtract(nz(gl.getDebitAmount()))
                            : nz(gl.getDebitAmount()).subtract(nz(gl.getCreditAmount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (movement.compareTo(BigDecimal.ZERO) == 0) continue;

            // Zero this account's contribution by posting the opposite side of its movement -
            // same four cases as the original single-posting version, just building a line
            // instead of posting immediately.
            if (creditNormal) {
                if (movement.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(LedgerLine.debit(account.getId(), movement));
                } else {
                    lines.add(LedgerLine.credit(account.getId(), movement.negate()));
                }
            } else {
                if (movement.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(LedgerLine.credit(account.getId(), movement));
                } else {
                    lines.add(LedgerLine.debit(account.getId(), movement.negate()));
                }
            }
            total = total.add(movement);
        }
        return total;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDateInClosedPeriod(Long companyId, LocalDate date) {
        return periodLockChecker.isDateInClosedPeriod(companyId, date);
    }

    private AccountingPeriod findInTenant(Long id, Long companyId) {
        return periodRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Accounting period not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
