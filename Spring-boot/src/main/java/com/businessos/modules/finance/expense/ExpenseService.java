package com.businessos.modules.finance.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpenseService {

    ExpenseResponse create(ExpenseRequest request);
    ExpenseResponse getById(Long id);
    Page<ExpenseResponse> getAll(Pageable pageable);
    Page<ExpenseResponse> getByStatus(ExpenseStatus status, Pageable pageable);
    Page<ExpenseResponse> getByVendorName(String vendorName, Pageable pageable);
    Page<ExpenseResponse> getMyExpenses(Long employeeId, Pageable pageable);
    ExpenseResponse update(Long id, ExpenseRequest request);
    /** Returns a non-blocking budget warning (or null) - see BudgetService.warningFor. */
    String approveExpense(Long id, String approvalNotes);
    void rejectExpense(Long id, String reason);
    void markAsPaid(Long id, String reimbursementMethod, String referenceNumber);
    void delete(Long id);
    /** AI micro-assist: turns rough notes into a draft title + description. Nothing is saved. */
    ExpenseComposeResponse composeEntry(ExpenseComposeRequest request);
}