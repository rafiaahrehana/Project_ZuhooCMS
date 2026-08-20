package com.businessos.modules.hrm.payroll.loan;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class LoanAdvanceResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LoanAdvance.Type type;
    private BigDecimal principalAmount;
    private LocalDate disbursedDate;
    private BigDecimal monthlyInstallment;
    private BigDecimal remainingBalance;
    private LoanAdvance.Status status;
    private String reason;
    private String notes;
    private LocalDateTime createdAt;
}
