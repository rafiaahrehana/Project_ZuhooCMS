package com.businessos.modules.finance.budget;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.finance.expense.ExpenseRepository;
import com.businessos.modules.finance.expense.ExpenseStatus;
import com.businessos.modules.finance.vendor.VendorBillRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    @Qualifier("financeExpenseRepository")
    private final ExpenseRepository expenseRepository;
    private final VendorBillRepository vendorBillRepository;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @Transactional
    public BudgetDtos.BudgetResponse create(BudgetDtos.BudgetRequest request) {
        authorizationService.checkPermission(PermissionCode.BUDGET_MANAGE);
        Long companyId = requireCompanyId();
        if (budgetRepository.existsByCompanyIdAndCategoryIgnoreCaseAndFiscalYear(
                companyId, request.getCategory().trim(), request.getFiscalYear())) {
            throw new BadRequestException("A budget for this category and fiscal year already exists");
        }
        Budget budget = Budget.builder()
                .companyId(companyId)
                .category(request.getCategory().trim())
                .fiscalYear(request.getFiscalYear())
                .amount(request.getAmount())
                .notes(request.getNotes())
                .build();
        budget = budgetRepository.save(budget);
        return toResponse(budget);
    }

    @Transactional
    public BudgetDtos.BudgetResponse update(Long id, BudgetDtos.BudgetRequest request) {
        authorizationService.checkPermission(PermissionCode.BUDGET_MANAGE);
        Budget budget = findInTenant(id);
        budget.setCategory(request.getCategory().trim());
        budget.setFiscalYear(request.getFiscalYear());
        budget.setAmount(request.getAmount());
        budget.setNotes(request.getNotes());
        budget = budgetRepository.save(budget);
        return toResponse(budget);
    }

    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.BUDGET_MANAGE);
        Budget budget = findInTenant(id);
        budget.softDelete();
        budgetRepository.save(budget);
    }

    /** All budgets for a fiscal year with live actual-vs-budget rollups. */
    @Transactional(readOnly = true)
    public List<BudgetDtos.BudgetResponse> listForYear(int fiscalYear) {
        authorizationService.checkPermission(PermissionCode.BUDGET_VIEW);
        Long companyId = requireCompanyId();
        return budgetRepository.findByCompanyIdAndFiscalYearOrderByCategoryAsc(companyId, fiscalYear)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Deliberately NOT gated by BUDGET_VIEW - this doubles as a cross-module picker
     * so the Expense form can suggest matching category names, for anyone who can
     * log an expense but may not otherwise have budget visibility. */
    @Transactional(readOnly = true)
    public List<String> listCategories() {
        return budgetRepository.findDistinctCategoriesByCompanyId(requireCompanyId());
    }

    private BudgetDtos.BudgetResponse toResponse(Budget budget) {
        LocalDate[] range = fiscalYearRange(budget.getCompanyId(), budget.getFiscalYear());
        BigDecimal actual = expenseRepository.sumByCategoryAndDateRange(
                budget.getCompanyId(), budget.getCategory(), range[0], range[1],
                List.of(ExpenseStatus.APPROVED, ExpenseStatus.PAID));
        if (actual == null) actual = BigDecimal.ZERO;
        actual = actual.add(vendorBillSpend(budget.getCompanyId(), budget.getCategory(), range[0], range[1]));

        BigDecimal remaining = budget.getAmount().subtract(actual);
        double usedPercent = budget.getAmount().compareTo(BigDecimal.ZERO) > 0
                ? actual.multiply(BigDecimal.valueOf(100)).divide(budget.getAmount(), 1, java.math.RoundingMode.HALF_UP).doubleValue()
                : 0;

        return BudgetDtos.BudgetResponse.builder()
                .id(budget.getId())
                .category(budget.getCategory())
                .fiscalYear(budget.getFiscalYear())
                .amount(budget.getAmount())
                .notes(budget.getNotes())
                .actualSpend(actual)
                .remaining(remaining)
                .usedPercent(usedPercent)
                .overBudget(remaining.compareTo(BigDecimal.ZERO) < 0)
                .build();
    }

    /**
     * Non-blocking budget check used when an expense is approved: returns a human
     * warning if approving `amountBeingAdded` in this category would cross 80% of, or
     * exceed, the category's budget for the fiscal year containing `date`. Null when
     * there's no budget for the category or spending is comfortably inside it.
     */
    @Transactional(readOnly = true)
    public String warningFor(Long companyId, String category, LocalDate date, BigDecimal amountBeingAdded) {
        if (category == null || category.isBlank() || date == null) return null;
        int fiscalYear = fiscalYearContaining(companyId, date);
        Budget budget = budgetRepository
                .findByCompanyIdAndCategoryIgnoreCaseAndFiscalYear(companyId, category.trim(), fiscalYear)
                .orElse(null);
        if (budget == null || budget.getAmount().compareTo(BigDecimal.ZERO) <= 0) return null;

        LocalDate[] range = fiscalYearRange(companyId, fiscalYear);
        BigDecimal actual = expenseRepository.sumByCategoryAndDateRange(
                companyId, category.trim(), range[0], range[1],
                List.of(ExpenseStatus.APPROVED, ExpenseStatus.PAID));
        if (actual == null) actual = BigDecimal.ZERO;
        actual = actual.add(vendorBillSpend(companyId, category.trim(), range[0], range[1]));
        BigDecimal afterApproval = actual.add(amountBeingAdded != null ? amountBeingAdded : BigDecimal.ZERO);

        if (afterApproval.compareTo(budget.getAmount()) > 0) {
            return "Over budget: \"" + budget.getCategory() + "\" FY" + fiscalYear + " spend becomes "
                    + afterApproval + " of " + budget.getAmount() + " budgeted ("
                    + afterApproval.subtract(budget.getAmount()) + " over).";
        }
        BigDecimal eightyPercent = budget.getAmount().multiply(new BigDecimal("0.8"));
        if (afterApproval.compareTo(eightyPercent) >= 0) {
            return "Approaching budget: \"" + budget.getCategory() + "\" FY" + fiscalYear + " spend becomes "
                    + afterApproval + " of " + budget.getAmount() + " budgeted.";
        }
        return null;
    }

    /**
     * A budget's category is a free-text string matched against Expense.category;
     * VendorBill has no such field, but does carry an optional expenseAccount -
     * matched here by account name, the closest bridge between the two without a
     * schema change. Bills posted to the generic fallback account (no
     * expenseAccount set) can't be attributed to any one budget category, same as
     * before this fix - they simply don't count toward any budget, not a
     * regression.
     *
     * Previously omitted entirely: a "Software" budget could be blown past by
     * approved vendor bills against the same GL account while the Budgets page
     * kept showing only the (much smaller) Expense-claim total.
     */
    private BigDecimal vendorBillSpend(Long companyId, String category, LocalDate start, LocalDate end) {
        BigDecimal sum = vendorBillRepository.sumByExpenseAccountNameAndDateRange(companyId, category, start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /** Which fiscal year (by its start year) contains this date, per the company's start month. */
    private int fiscalYearContaining(Long companyId, LocalDate date) {
        int startMonth = companyRepository.findById(companyId)
                .map(Company::getFiscalYearStartMonth)
                .filter(m -> m != null && m >= 1 && m <= 12)
                .orElse(1);
        return date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1;
    }

    /** The fiscal year's calendar window, honoring the company's fiscalYearStartMonth. */
    private LocalDate[] fiscalYearRange(Long companyId, int fiscalYear) {
        int startMonth = companyRepository.findById(companyId)
                .map(Company::getFiscalYearStartMonth)
                .filter(m -> m != null && m >= 1 && m <= 12)
                .orElse(1);
        LocalDate start = LocalDate.of(fiscalYear, startMonth, 1);
        return new LocalDate[]{start, start.plusMonths(12).minusDays(1)};
    }

    private Budget findInTenant(Long id) {
        return budgetRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
