package com.businessos.modules.hrm.payroll.loan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateLoanRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull
    private LoanAdvance.Type type;

    @NotNull
    @DecimalMin(value = "0.01", message = "Principal amount must be greater than zero")
    private BigDecimal principalAmount;

    @NotNull
    private LocalDate disbursedDate;

    @NotNull
    @DecimalMin(value = "0.01", message = "Monthly installment must be greater than zero")
    private BigDecimal monthlyInstallment;

    private String reason;
    private String notes;
}
