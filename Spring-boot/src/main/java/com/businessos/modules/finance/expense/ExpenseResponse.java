package com.businessos.modules.finance.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private Long companyId;
    private String expenseNumber;
    private String title;
    private String currency;
    private Long employeeId;
    private String submittedByName;
    private String description;
    private BigDecimal amount;
    private String vendorName;
    private String category;
    private Long expenseAccountId;
    private String expenseAccountName;
    private LocalDate expenseDate;
    private String receiptUrl;
    private ExpenseStatus status;
    private String approvedByName;
    private String approvalNotes;
    private LocalDateTime submittedAt;
    private String notes;
    private LocalDate reimbursedDate;
    private String reimbursementMethod;
    private String referenceNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
