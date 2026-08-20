package com.businessos.modules.finance.expense;


public class ExpenseMapper {

    public static ExpenseResponse toResponse(Expense entity) {
        if (entity == null) return null;

        return ExpenseResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .expenseNumber(entity.getExpenseNumber())
                .title(entity.getTitle())
                .currency(entity.getCurrency())
                .employeeId(entity.getSubmittedBy() != null ? entity.getSubmittedBy().getId() : null)
                .submittedByName(entity.getSubmittedBy() != null ? entity.getSubmittedBy().getFullName() : null)
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .vendorName(entity.getVendorName() != null ? entity.getVendorName(): null)
                .category(entity.getCategory())
                .expenseAccountId(entity.getExpenseAccount() != null ? entity.getExpenseAccount().getId() : null)
                .expenseAccountName(entity.getExpenseAccount() != null ? entity.getExpenseAccount().getAccountName() : null)
                .expenseDate(entity.getExpenseDate())
                .receiptUrl(entity.getReceiptUrl())
                .status(entity.getStatus())
                .approvedByName(entity.getApprovedBy() != null ? entity.getApprovedBy().getFullName() : null)
                .approvalNotes(entity.getApprovalNotes())
                .submittedAt(entity.getSubmittedAt())
                .notes(entity.getNotes())
                .reimbursedDate(entity.getReimbursedDate())
                .reimbursementMethod(entity.getReimbursementMethod())
                .referenceNumber(entity.getReferenceNumber())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public static Expense toEntity(ExpenseRequest request) {
        if (request == null) return null;

        return Expense.builder()
                .title(request.getTitle())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .amount(request.getAmount())
                .category(request.getCategory())
                .expenseDate(request.getExpenseDate())
                .receiptUrl(request.getReceiptUrl())
                .notes(request.getNotes())
                .referenceNumber(request.getReferenceNumber())
                .build();
    }
}
