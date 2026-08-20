package com.businessos.modules.hrm.payroll.salarysheet;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * The whole company's salary sheet for one month, computed live from salary
 * structures, attendance and the company's payroll settings.
 *
 * Nothing here is stored: it is a view of what the month currently looks like,
 * so it moves as attendance is corrected. Running payroll is the separate act
 * that freezes these figures into Payroll rows.
 */
@Data
@Builder
public class SalarySheetResponse {

    private int payMonth;
    private int payYear;

    /** Echoed so the sheet can explain how a day rate was reached. */
    private String perDayBasis;
    private int perDayDivisor;
    private boolean overtimeEnabled;
    private BigDecimal overtimeMultiplier;

    private List<SalarySheetRow> rows;

    // ── Column totals, matching the footer of the sheet ──────
    private BigDecimal totalBasic;
    private BigDecimal totalHouseRent;
    private BigDecimal totalMedical;
    private BigDecimal totalTransport;
    private BigDecimal totalFood;
    private BigDecimal totalSpecial;
    private BigDecimal totalOvertimeHours;
    private BigDecimal totalOvertimePayment;
    private BigDecimal totalBonus;
    private BigDecimal totalOtherEarnings;
    private BigDecimal totalOtherDeductions;
    private BigDecimal totalGrossEarnings;
    private int totalAbsentDays;
    private BigDecimal totalAbsentDeduction;
    private BigDecimal totalTax;
    private BigDecimal totalProvidentFund;
    private BigDecimal totalDeductions;
    private BigDecimal totalNetPayable;
}
