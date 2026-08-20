package com.businessos.modules.finance.expense;

public enum ExpenseStatus {
    PENDING("Pending approval"),
    APPROVED("Approved by manager"),
    REJECTED("Rejected"),
    PAID("Reimbursement paid"),
    CANCELLED("Cancelled");

    private final String description;

    ExpenseStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
