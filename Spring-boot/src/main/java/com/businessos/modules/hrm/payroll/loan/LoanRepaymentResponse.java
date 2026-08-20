package com.businessos.modules.hrm.payroll.loan;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class LoanRepaymentResponse {
    private Long id;
    private Long payrollId;
    private Integer payMonth;
    private Integer payYear;
    private BigDecimal amount;
    private LocalDate paidDate;
    private BigDecimal balanceAfter;
}
