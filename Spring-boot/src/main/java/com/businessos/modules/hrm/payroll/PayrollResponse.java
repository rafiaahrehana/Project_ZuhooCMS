package com.businessos.modules.hrm.payroll;

import com.businessos.enums.PayrollStatus;
import com.businessos.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PayrollResponse {
    private Long id;
    private int payMonth;
    private int payYear;
    private BigDecimal basicSalary;
    private BigDecimal houseRent;
    private BigDecimal medicalAllowance;
    private BigDecimal transportAllowance;
    private BigDecimal foodAllowance;
    private BigDecimal specialAllowance;
    private BigDecimal bonus;
    private BigDecimal billableHours;
    private BigDecimal billableRate;
    private BigDecimal billablePay;
    private BigDecimal overtimeHours;
    private BigDecimal overtimeRate;
    private BigDecimal overtimePay;
    private BigDecimal deductions;
    private BigDecimal taxDeduction;
    private BigDecimal insuranceDeduction;
    private BigDecimal providentFundDeduction;
    private BigDecimal attendanceDeduction;
    private BigDecimal otherEarnings;
    private BigDecimal otherDeductions;
    private Long loanAdvanceId;
    private BigDecimal loanDeductionAmount;
    private Integer absentDays;
    private BigDecimal netSalary;
    private PayrollStatus status;
    private String paymentReference;
    private PaymentMethod paymentMethod;
    private LocalDate paidAt;
    private String notes;
    private Long employeeId;
    private String employeeName;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime createdAt;
}
