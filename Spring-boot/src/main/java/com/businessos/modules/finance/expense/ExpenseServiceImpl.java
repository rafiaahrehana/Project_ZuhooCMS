package com.businessos.modules.finance.expense;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.DefaultAccountResolver;
import com.businessos.modules.finance.generalledger.GeneralLedgerService;
import com.businessos.modules.finance.generalledger.GlReferenceType;
import com.businessos.modules.finance.generalledger.LedgerLine;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ForbiddenException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.ai.enums.AiFeature;
import com.businessos.modules.ai.prompt.ExpenseEntryPromptBuilder;
import com.businessos.modules.ai.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service("financeExpenseService")
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private static final ObjectMapper COMPOSE_MAPPER = new ObjectMapper();

    private final ExpenseRepository expenseRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final SecurityUtil securityUtil;
    private final GeneralLedgerService glService;
    private final DefaultAccountResolver accountResolver;
    private final AuthorizationService authorizationService;
    private final com.businessos.modules.finance.chartofaccounts.ChartOfAccountRepository coaRepository;
    private final com.businessos.modules.finance.budget.BudgetService budgetService;
    private final AiService aiService;

    /** Resolves + validates an optional COA expense account (must exist in-tenant and be EXPENSE type). */
    private ChartOfAccount resolveExpenseAccount(Long companyId, Long accountId) {
        if (accountId == null) return null;
        ChartOfAccount account = coaRepository.findByIdAndCompanyId(accountId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense account not found: " + accountId));
        if (account.getType() != com.businessos.modules.finance.chartofaccounts.AccountType.EXPENSE) {
            throw new BadRequestException("Account " + account.getAccountCode() + " is " + account.getType()
                    + " - expenses must post to an EXPENSE account");
        }
        return account;
    }

    @Override
    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Long companyId = securityUtil.getCurrentCompanyId();
        User currentUser = securityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        Long currentUserId = currentUser.getId();

        Employee employee = null;
        Long reqEmployeeId = request.getEmployeeId();
        if (reqEmployeeId != null) {
            employee = employeeRepository.findByIdAndCompanyId(reqEmployeeId, companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        } else if (currentUser.isTenantUser()) {
            employee = employeeRepository.findByUserId(currentUserId).orElse(null);
        }



        // Generate unique expense number
        String expenseNumber = generateExpenseNumber(companyId);

        Expense expense = Expense.builder()
                .companyId(companyId)
                .expenseNumber(expenseNumber)
                .title(request.getTitle() != null ? request.getTitle() : "Expense " + expenseNumber)
                .currency(request.getCurrency() != null ? request.getCurrency() : "BDT")
                .submittedBy(employee)
                .description(request.getDescription())
                .amount(request.getAmount())
                .vendorName(request.getVendorName())
                .category(request.getCategory())
                .expenseAccount(resolveExpenseAccount(companyId, request.getExpenseAccountId()))
                .expenseDate(request.getExpenseDate())
                .receiptUrl(request.getReceiptUrl())
                .status(ExpenseStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .notes(request.getNotes())
                .referenceNumber(request.getReferenceNumber())
                .build();

        expense = expenseRepository.save(expense);
        return ExpenseMapper.toResponse(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long id) {
        Expense expense = findInTenant(id);
        // Platform expenses have no CustomRole to check EXPENSE_VIEW against - the
        // controller's role-based @PreAuthorize (PLATFORM_ACCOUNTANT/SUPER_ADMIN)
        // already gates this for that branch (mirrors SupportTicketServiceImpl.getAll).
        if (isPlatformCaller()) {
            return ExpenseMapper.toResponse(expense);
        }
        if (!authorizationService.hasPermission(PermissionCode.EXPENSE_VIEW)) {
            requireOwnExpense(expense);
        }
        return ExpenseMapper.toResponse(expense);
    }

    private boolean isPlatformCaller() {
        User current = securityUtil.getCurrentUser();
        return current != null && current.isPlatformUser();
    }

    // Platform expenses (SaaS provider's own operating costs) are stored with a null
    // companyId - they belong to no tenant, so they're looked up/listed separately
    // from tenant expenses rather than by the caller's (nonexistent) company id.
    private Expense findInTenant(Long id) {
        if (isPlatformCaller()) {
            return expenseRepository.findByIdAndCompanyIdIsNull(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        }
        return expenseRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }

    private void requireOwnExpense(Expense expense) {
        User currentUser = securityUtil.getCurrentUser();
        Employee currentEmployee = currentUser != null
                ? employeeRepository.findByUserId(currentUser.getId()).orElse(null)
                : null;
        if (currentEmployee == null || expense.getSubmittedBy() == null
                || !expense.getSubmittedBy().getId().equals(currentEmployee.getId())) {
            throw new ForbiddenException("Access denied: you can only access your own expenses");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getAll(Pageable pageable) {
        if (isPlatformCaller()) {
            return expenseRepository.findByCompanyIdIsNull(pageable)
                    .map(ExpenseMapper::toResponse);
        }
        authorizationService.checkPermission(PermissionCode.EXPENSE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return expenseRepository.findByCompanyId(companyId, pageable)
                .map(ExpenseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getByStatus(ExpenseStatus status, Pageable pageable) {
        if (isPlatformCaller()) {
            return expenseRepository.findByCompanyIdIsNullAndStatus(status, pageable)
                    .map(ExpenseMapper::toResponse);
        }
        authorizationService.checkPermission(PermissionCode.EXPENSE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return expenseRepository.findByCompanyIdAndStatus(companyId, status, pageable)
                .map(ExpenseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getByVendorName(String vendorName, Pageable pageable) {
        if (isPlatformCaller()) {
            return expenseRepository.findByCompanyIdIsNullAndVendorName(vendorName, pageable)
                    .map(ExpenseMapper::toResponse);
        }
        authorizationService.checkPermission(PermissionCode.EXPENSE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return expenseRepository.findByCompanyIdAndVendorName(companyId, vendorName, pageable)
                .map(ExpenseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getMyExpenses(Long employeeId, Pageable pageable) {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (employeeId == null) {
            User currentUser = securityUtil.getCurrentUser();
            if (currentUser == null) {
                throw new ResourceNotFoundException("User not authenticated");
            }
            employeeId = employeeRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"))
                    .getId();
        }
        return expenseRepository.findByCompanyIdAndSubmittedById(companyId, employeeId, pageable)
                .map(ExpenseMapper::toResponse);
    }

    @Override
    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = findInTenant(id);

        if (!isPlatformCaller() && !authorizationService.hasPermission(PermissionCode.EXPENSE_UPDATE)) {
            requireOwnExpense(expense);
        }

        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new BadRequestException("Can only update pending expenses");
        }

        if (request.getTitle() != null) expense.setTitle(request.getTitle());
        if (request.getCurrency() != null) expense.setCurrency(request.getCurrency());
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setCategory(request.getCategory());
        expense.setExpenseAccount(resolveExpenseAccount(expense.getCompanyId(), request.getExpenseAccountId()));
        expense.setExpenseDate(request.getExpenseDate());
        expense.setReceiptUrl(request.getReceiptUrl());
        expense.setNotes(request.getNotes());

        if (request.getVendorName() != null) {
            expense.setVendorName(request.getVendorName());
        }

        expense = expenseRepository.save(expense);
        return ExpenseMapper.toResponse(expense);
    }

    @Override
    @Transactional
    public String approveExpense(Long id, String approvalNotes) {
        if (!isPlatformCaller()) {
            authorizationService.checkPermission(PermissionCode.EXPENSE_APPROVE);
        }
        Expense expense = findInTenant(id);

        User currentUser = securityUtil.getCurrentUser();
        if (currentUser == null) {
            throw new ResourceNotFoundException("User not authenticated");
        }
        Long currentUserId = currentUser.getId();
        User approver = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Maker-checker: you can't approve your own expense claim - the whole point of
        // the approval step is a second person's eyes on the money.
        if (expense.getSubmittedBy() != null && expense.getSubmittedBy().getUser() != null
                && expense.getSubmittedBy().getUser().getId().equals(currentUserId)) {
            throw new BadRequestException("You submitted this expense - a different user must approve it");
        }

        expense.approve(approver);
        expense.setApprovalNotes(approvalNotes);
        expenseRepository.save(expense);

        // Vendor bills post Dr Expense / Cr Payable at approval; expense claims
        // posted nothing until actually paid - a balance sheet pulled between
        // approval and reimbursement understated period-end liabilities for this
        // spend channel but not the other. Mirrors VendorBillService.approve().
        if (expense.getCompanyId() != null) {
            Long companyId = expense.getCompanyId();
            String description = "Expense approved: " + expense.getTitle()
                    + (expense.getCategory() != null ? " (" + expense.getCategory() + ")" : "")
                    + " - " + expense.getExpenseNumber();
            LocalDate transactionDate = expense.getExpenseDate() != null ? expense.getExpenseDate() : LocalDate.now();
            ChartOfAccount expenseAccount = expense.getExpenseAccount() != null
                    ? expense.getExpenseAccount()
                    : accountResolver.operatingExpenses(companyId);
            ChartOfAccount ap = accountResolver.accountsPayable(companyId);
            glService.recordBalancedTransaction(companyId, java.util.List.of(
                            LedgerLine.debit(expenseAccount.getId(), expense.getAmount()),
                            LedgerLine.credit(ap.getId(), expense.getAmount())),
                    description, GlReferenceType.EXPENSE, expense.getId(), expense.getExpenseNumber(), transactionDate);
        }

        // Non-blocking: the approver is warned when this pushes the category over (or
        // near) its budget, but the approval still stands - real companies want a
        // human judgment call here, not a hard stop. Platform expenses have no company
        // and therefore no budgets.
        if (expense.getCompanyId() != null) {
            return budgetService.warningFor(expense.getCompanyId(), expense.getCategory(),
                    expense.getExpenseDate(), expense.getAmount());
        }
        return null;
    }

    @Override
    @Transactional
    public void rejectExpense(Long id, String reason) {
        if (!isPlatformCaller()) {
            authorizationService.checkPermission(PermissionCode.EXPENSE_REJECT);
        }
        Expense expense = findInTenant(id);
        if (expense.getStatus() == ExpenseStatus.PAID) {
            throw new BadRequestException("Cannot reject a paid expense");
        }
        boolean wasApproved = expense.getStatus() == ExpenseStatus.APPROVED;
        expense.reject();
        expense.setApprovalNotes(reason);
        expenseRepository.save(expense);

        // approveExpense() now posts a real Dr Expense / Cr Payable liability -
        // rejecting an already-approved expense must reverse it, or the liability
        // sits in Accounts Payable forever with nothing left to pay it off. Same
        // reversal shape as VendorBillService.cancel()'s wasPosted branch.
        if (wasApproved && expense.getCompanyId() != null) {
            Long companyId = expense.getCompanyId();
            String description = "Expense " + expense.getExpenseNumber() + " rejected after approval - reversal";
            ChartOfAccount expenseAccount = expense.getExpenseAccount() != null
                    ? expense.getExpenseAccount()
                    : accountResolver.operatingExpenses(companyId);
            ChartOfAccount ap = accountResolver.accountsPayable(companyId);
            glService.recordBalancedTransaction(companyId, java.util.List.of(
                            LedgerLine.credit(expenseAccount.getId(), expense.getAmount()),
                            LedgerLine.debit(ap.getId(), expense.getAmount())),
                    description, GlReferenceType.EXPENSE, expense.getId(), expense.getExpenseNumber(), LocalDate.now());
        }
    }

    @Override
    @Transactional
    public void markAsPaid(Long id, String reimbursementMethod, String referenceNumber) {
        if (!isPlatformCaller()) {
            authorizationService.checkPermission(PermissionCode.EXPENSE_APPROVE);
        }
        Expense expense = findInTenant(id);
        expense.markAsPaid(reimbursementMethod, referenceNumber);
        expenseRepository.save(expense);

        // The expense itself was already recognized as Dr Expense / Cr Payable at
        // approval time (above) - paying it now clears that liability rather than
        // re-recognizing the expense a second time: Dr Accounts Payable / Cr Cash,
        // the same shape as VendorBillService.recordPayment().
        Long companyId = expense.getCompanyId();
        String description = "Expense reimbursed: " + expense.getTitle()
                + (expense.getCategory() != null ? " (" + expense.getCategory() + ")" : "")
                + " - " + expense.getExpenseNumber();

        ChartOfAccount ap = accountResolver.accountsPayable(companyId);
        ChartOfAccount cash = accountResolver.cash(companyId);
        glService.recordBalancedTransaction(companyId, java.util.List.of(
                        LedgerLine.debit(ap.getId(), expense.getAmount()),
                        LedgerLine.credit(cash.getId(), expense.getAmount())),
                description, GlReferenceType.EXPENSE, expense.getId(), expense.getExpenseNumber(), expense.getReimbursedDate());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Expense expense = findInTenant(id);
        if (!isPlatformCaller() && !authorizationService.hasPermission(PermissionCode.EXPENSE_DELETE)) {
            requireOwnExpense(expense);
        }
        if (expense.getStatus() == ExpenseStatus.PAID || expense.getStatus() == ExpenseStatus.APPROVED) {
            // APPROVED now posts a real Dr Expense / Cr Payable liability - deleting
            // it would leave that GL entry dangling with nothing left to reference it.
            throw new BadRequestException("Cannot delete a " + expense.getStatus()
                    + " expense - it has GL entries. Reject it first if it needs to be undone.");
        }
        expense.softDelete();
        expenseRepository.save(expense);
    }

    @Override
    public ExpenseComposeResponse composeEntry(ExpenseComposeRequest request) {
        String prompt = ExpenseEntryPromptBuilder.builder()
            .setVendorName(request.getVendorName())
            .setAmount(request.getAmount())
            .setCategory(request.getCategory())
            .setRoughNotes(request.getRoughNotes())
            .build();

        String raw = aiService.generateRaw(AiFeature.EXPENSE_ENTRY, prompt);
        return parseCompose(raw, request.getRoughNotes());
    }

    private ExpenseComposeResponse parseCompose(String raw, String fallbackNotes) {
        ExpenseComposeResponse response = new ExpenseComposeResponse();
        try {
            String cleaned = raw.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceFirst("^```[a-zA-Z]*\\n?", "").replaceFirst("```\\s*$", "");
            }
            JsonNode node = COMPOSE_MAPPER.readTree(cleaned);
            response.setTitle(node.path("title").asText(null));
            response.setDescription(node.path("description").asText(null));
        } catch (Exception ignored) {
            // Model didn't return valid JSON despite instructions - fall back to the
            // raw text as the description rather than failing the whole request.
        }
        if (response.getTitle() == null || response.getTitle().isBlank()) {
            response.setTitle(fallbackNotes.length() > 60
                ? fallbackNotes.substring(0, 57) + "..." : fallbackNotes);
        }
        if (response.getDescription() == null || response.getDescription().isBlank()) {
            response.setDescription(raw);
        }
        return response;
    }

    private String generateExpenseNumber(Long companyId) {
        int year = LocalDate.now().getYear();
        String prefix = "EXP-" + year + "-";
        // companyId is null for platform expenses - the tenant query's "= :companyId"
        // never matches NULL rows, so the sequence needs its own IS NULL lookup or
        // every platform expense would collide on 000001.
        String maxNumber = (companyId == null
                ? expenseRepository.findMaxExpenseNumberByPlatformAndPrefix(prefix)
                : expenseRepository.findMaxExpenseNumberByCompanyAndPrefix(companyId, prefix))
                .orElse(prefix + "000000");
        long sequence = Long.parseLong(maxNumber.substring(prefix.length())) + 1;
        return String.format("%s%06d", prefix, sequence);
    }
}