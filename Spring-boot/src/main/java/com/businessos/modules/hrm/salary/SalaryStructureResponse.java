package com.businessos.modules.hrm.salary;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class SalaryStructureResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private BigDecimal grossSalary;
    private BigDecimal basicSalary;
    private BigDecimal houseRent;
    private BigDecimal medicalAllowance;
    private BigDecimal transportAllowance;
    private BigDecimal foodAllowance;
    private BigDecimal specialAllowance;
    private BigDecimal providentFund;
    private BigDecimal taxDeduction;
    private BigDecimal netSalary;
    private String notes;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime createdAt;
}
