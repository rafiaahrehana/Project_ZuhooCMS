package com.businessos.modules.hrm.payroll;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePayrollRequest {
    @NotNull(message = "Employee ID is required")
    private Long employeeId;
    @NotNull
    @Min(1)
    @Max(12)
    private Integer payMonth;
    @NotNull
    @Min(2020)
    private Integer payYear;

    // basicSalary/houseRent/medicalAllowance/transportAllowance are optional - if
    // omitted, PayrollServiceImpl pulls them from the employee's active
    // SalaryStructure for this period instead of requiring HR to retype them
    // every month. Only required if the employee has no salary structure set up.
    private BigDecimal basicSalary;
    private BigDecimal houseRent;
    private BigDecimal medicalAllowance;
    private BigDecimal transportAllowance;
    private BigDecimal foodAllowance;
    private BigDecimal specialAllowance;
    private BigDecimal bonus;
    private BigDecimal deductions;
    private BigDecimal taxDeduction;
    private BigDecimal insuranceDeduction;
    private BigDecimal providentFundDeduction;
    private String notes;
}
