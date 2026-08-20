package com.businessos.modules.hrm.payroll.salarysheet;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** One employee's line on the salary sheet for a pay month. */
@Data
@Builder
public class SalarySheetRow {

    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    /** Designation, falling back to the free-text job title. */
    private String position;

    // ── Earnings ─────────────────────────────────────────────
    private BigDecimal basic;
    private BigDecimal houseRent;
    private BigDecimal medical;
    private BigDecimal transport;
    private BigDecimal food;
    private BigDecimal special;

    private BigDecimal overtimeHours;
    private BigDecimal overtimePayment;

    /** Fixed components plus overtime — what the employee earned before deductions. */
    private BigDecimal grossEarnings;

    // ── Deductions ───────────────────────────────────────────
    private int absentDays;
    private BigDecimal absentDeduction;
    private BigDecimal tax;
    private BigDecimal providentFund;
    private BigDecimal totalDeductions;

    private BigDecimal netPayable;

    /**
     * Set when the employee has no salary structure covering this month, in
     * which case every figure above is zero. Shown on the row rather than the
     * employee being dropped, so nobody silently disappears from the sheet.
     */
    private String note;

    // Payment state, from the period.s Payroll row when one exists.
    private Long payrollId;
    private String paymentStatus;
    private String paymentMethod;
    private String department;

    // Structure extra components (loan EMI, internet, ...), frozen the same
    // way payroll freezes them.
    private java.math.BigDecimal otherEarnings;
    private java.math.BigDecimal otherDeductions;

    /** Month bonus - only ever non-zero on PAYROLL-sourced rows. */
    private java.math.BigDecimal bonus;

    /**
     * PAYROLL when the row restates the period's actual payroll record
     * (the real register - what was/will be paid), PROJECTED when payroll
     * hasn't been generated and the row is a live estimate from the
     * structure and attendance.
     */
    private String source;
}
