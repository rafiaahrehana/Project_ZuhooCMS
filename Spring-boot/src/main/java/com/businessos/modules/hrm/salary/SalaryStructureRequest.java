package com.businessos.modules.hrm.salary;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SalaryStructureRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;
    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;
    @NotNull(message = "Gross salary is required")
    @DecimalMin(value = "0.01")
    private BigDecimal grossSalary;
    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.01")
    private BigDecimal basicSalary;
    private BigDecimal houseRent;
    private BigDecimal medicalAllowance;
    private BigDecimal transportAllowance;
    private BigDecimal foodAllowance;
    private BigDecimal specialAllowance;
    private BigDecimal providentFund;
    private BigDecimal taxDeduction;
    private String notes;
}
