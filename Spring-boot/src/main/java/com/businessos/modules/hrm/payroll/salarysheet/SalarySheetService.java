package com.businessos.modules.hrm.payroll.salarysheet;

import com.businessos.enums.SalaryBase;
import com.businessos.modules.hrm.attendance.attendance.AttendanceRepository;
import com.businessos.modules.hrm.attendance.attendance.AttendanceStatus;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.payroll.loan.LoanAdvance;
import com.businessos.modules.hrm.payroll.loan.LoanAdvanceRepository;
import com.businessos.modules.hrm.payroll.settings.PayrollSettings;
import com.businessos.modules.hrm.payroll.settings.PayrollSettingsService;
import com.businessos.modules.hrm.salary.SalaryStructure;
import com.businessos.modules.hrm.salary.SalaryStructureRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the company's salary sheet for a month.
 *
 * Live rather than stored: absent days and overtime hours are read from
 * attendance every time, so correcting a check-in is reflected immediately
 * instead of needing payroll re-run. Payroll generation is what freezes these
 * numbers; until then this is simply what the month currently looks like.
 */
@Service
@RequiredArgsConstructor
public class SalarySheetService {

    private final EmployeeRepository employeeRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final AttendanceRepository attendanceRepository;
    private final PayrollSettingsService payrollSettingsService;
    private final LoanAdvanceRepository loanAdvanceRepository;
    private final SecurityUtil securityUtil;
    private final com.businessos.modules.hrm.payroll.PayrollRepository payrollRepository;
    private final com.businessos.modules.hrm.payroll.components.SalaryComponentService salaryComponentService;
    private final com.businessos.modules.hrm.leave.LeaveService leaveService;

    @Transactional(readOnly = true)
    public SalarySheetResponse build(int payMonth, int payYear) {
        if (payMonth < 1 || payMonth > 12) {
            throw new BadRequestException("Pay month must be between 1 and 12.");
        }
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context for the current user");
        }

        PayrollSettings settings = payrollSettingsService.getOrCreate(companyId);
        LocalDate start = LocalDate.of(payYear, payMonth, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        int divisor = payrollSettingsService.perDayDivisor(settings, payMonth, payYear);

        List<SalarySheetRow> rows = new ArrayList<>();
        for (Employee employee : employeeRepository.findByCompanyIdAndActiveTrue(companyId)) {
            rows.add(buildRow(employee, settings, payMonth, payYear, start, end));
        }
        rows.sort((a, b) -> a.getEmployeeName().compareToIgnoreCase(b.getEmployeeName()));

        return totalled(rows, payMonth, payYear, settings, divisor);
    }

    private SalarySheetRow buildRow(Employee employee, PayrollSettings settings,
                                    int payMonth, int payYear, LocalDate start, LocalDate end) {

        SalarySheetRow.SalarySheetRowBuilder row = SalarySheetRow.builder()
                .employeeId(employee.getId())
                .employeeNumber(employee.getEmployeeNumber())
                .employeeName(displayName(employee))
                .position(position(employee))
                .department(employee.getDepartment() != null ? employee.getDepartment().getName() : null);

        // Real-company rule: once payroll exists for the period, the sheet
        // restates the payroll register (the actuals - frozen overtime,
        // bonus, absence, components), NOT a fresh estimate. Before payroll
        // is generated the sheet is a live projection from the structure.
        var payrollOpt = payrollRepository.findByEmployeeIdAndPayMonthAndPayYear(
                employee.getId(), payMonth, payYear);
        if (payrollOpt.isPresent()) {
            var p = payrollOpt.get();
            BigDecimal earnExtras = orZero(p.getOtherEarnings()).add(orZero(p.getBillablePay()));
            BigDecimal dedExtras = orZero(p.getOtherDeductions()).add(orZero(p.getDeductions()))
                    .add(orZero(p.getInsuranceDeduction()));
            BigDecimal gross = orZero(p.getBasicSalary()).add(orZero(p.getHouseRent()))
                    .add(orZero(p.getMedicalAllowance())).add(orZero(p.getTransportAllowance()))
                    .add(orZero(p.getFoodAllowance())).add(orZero(p.getSpecialAllowance()))
                    .add(orZero(p.getBonus())).add(orZero(p.getOvertimePay())).add(earnExtras);
            // netPayable (below) already subtracts loanDeductionAmount - this line
            // didn't, so the official monthly salary register didn't reconcile
            // (gross - totalDeductions != netPayable for anyone with an active
            // loan) and the loan line was invisible on it entirely.
            BigDecimal totalDed = orZero(p.getAttendanceDeduction()).add(orZero(p.getTaxDeduction()))
                    .add(orZero(p.getProvidentFundDeduction())).add(orZero(p.getLoanDeductionAmount())).add(dedExtras);
            return row
                    .source("PAYROLL")
                    .payrollId(p.getId())
                    .paymentStatus(p.getStatus() != null ? p.getStatus().name() : null)
                    .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null)
                    .basic(orZero(p.getBasicSalary())).houseRent(orZero(p.getHouseRent()))
                    .medical(orZero(p.getMedicalAllowance())).transport(orZero(p.getTransportAllowance()))
                    .food(orZero(p.getFoodAllowance())).special(orZero(p.getSpecialAllowance()))
                    .bonus(orZero(p.getBonus()))
                    .overtimeHours(orZero(p.getOvertimeHours())).overtimePayment(orZero(p.getOvertimePay()))
                    .otherEarnings(earnExtras).otherDeductions(dedExtras)
                    .grossEarnings(gross)
                    .absentDays(p.getAbsentDays() != null ? p.getAbsentDays() : 0)
                    .absentDeduction(orZero(p.getAttendanceDeduction()))
                    .tax(orZero(p.getTaxDeduction())).providentFund(orZero(p.getProvidentFundDeduction()))
                    .totalDeductions(totalDed)
                    .netPayable(orZero(p.getNetSalary()))
                    .build();
        }
        row.source("PROJECTED").bonus(BigDecimal.ZERO);

        SalaryStructure structure = salaryStructureRepository
                .findActiveForEmployeeOnDate(employee.getId(), start)
                .orElse(null);

        int absentDays = (int) attendanceRepository.countByEmployeeIdAndStatusAndAttendanceDateBetween(
                employee.getId(), AttendanceStatus.ABSENT, start, end)
                // Same rule as payroll: approved UNPAID leave deducts like absence.
                + leaveService.unpaidLeaveDays(employee.getId(), payMonth, payYear);

        if (structure == null) {
            // Listed with zeroes and a note rather than omitted - an employee
            // missing from a salary sheet is a problem you want to see.
            return row
                    .basic(BigDecimal.ZERO).houseRent(BigDecimal.ZERO).medical(BigDecimal.ZERO)
                    .transport(BigDecimal.ZERO).food(BigDecimal.ZERO).special(BigDecimal.ZERO)
                    .overtimeHours(BigDecimal.ZERO).overtimePayment(BigDecimal.ZERO)
                    .otherEarnings(BigDecimal.ZERO).otherDeductions(BigDecimal.ZERO)
                    .grossEarnings(BigDecimal.ZERO)
                    .absentDays(absentDays).absentDeduction(BigDecimal.ZERO)
                    .tax(BigDecimal.ZERO).providentFund(BigDecimal.ZERO)
                    .totalDeductions(BigDecimal.ZERO).netPayable(BigDecimal.ZERO)
                    .note("No salary structure effective for this month")
                    .build();
        }

        BigDecimal basic = orZero(structure.getBasicSalary());
        BigDecimal houseRent = orZero(structure.getHouseRent());
        BigDecimal medical = orZero(structure.getMedicalAllowance());
        BigDecimal transport = orZero(structure.getTransportAllowance());
        BigDecimal food = orZero(structure.getFoodAllowance());
        BigDecimal special = orZero(structure.getSpecialAllowance());
        BigDecimal fixedGross = basic.add(houseRent).add(medical).add(transport).add(food).add(special);

        // ── Overtime ─────────────────────────────────────────
        BigDecimal overtimeHours = BigDecimal.ZERO;
        BigDecimal overtimePayment = BigDecimal.ZERO;
        if (settings.isOvertimeEnabled()) {
            overtimeHours = orZero(attendanceRepository.sumOvertimeHours(employee.getId(), start, end));
            if (overtimeHours.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal otBase = settings.getOvertimeBase() == SalaryBase.GROSS ? fixedGross : basic;
                BigDecimal perDay = payrollSettingsService.perDayRate(settings, otBase, payMonth, payYear);
                BigDecimal hoursPerDay = orZero(settings.getStandardHoursPerDay());
                if (hoursPerDay.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal hourly = perDay.divide(hoursPerDay, 4, RoundingMode.HALF_UP)
                            .multiply(orZero(settings.getOvertimeMultiplier()));
                    overtimePayment = hourly.multiply(overtimeHours).setScale(2, RoundingMode.HALF_UP);
                }
            }
        }

        // ── Absence ──────────────────────────────────────────
        BigDecimal absenceBase = settings.getAbsenceDeductionBase() == SalaryBase.BASIC ? basic : fixedGross;
        BigDecimal absentDeduction = BigDecimal.ZERO;
        if (absentDays > 0 && absenceBase.compareTo(BigDecimal.ZERO) > 0) {
            absentDeduction = payrollSettingsService.perDayRate(settings, absenceBase, payMonth, payYear)
                    .multiply(BigDecimal.valueOf(absentDays))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal tax = orZero(structure.getTaxDeduction());
        BigDecimal providentFund = orZero(structure.getProvidentFund());

        // Structure extra components, same sums payroll freezes.
        BigDecimal otherEarnings = salaryComponentService.sumExtras(structure.getId(),
                com.businessos.modules.hrm.payroll.components.SalaryComponent.ComponentType.EARNING);
        BigDecimal otherDeductions = salaryComponentService.sumExtras(structure.getId(),
                com.businessos.modules.hrm.payroll.components.SalaryComponent.ComponentType.DEDUCTION);

        // Same active-loan lookup PayrollServiceImpl.calculateLoanDue() uses when
        // actually generating payroll - without it, this "projected" preview
        // disagreed with the real Payroll row by exactly the loan installment,
        // contradicting its own documented promise that preview and actual never
        // disagree.
        BigDecimal loanDue = loanAdvanceRepository
                .findFirstByEmployeeIdAndStatus(employee.getId(), LoanAdvance.Status.ACTIVE)
                .map(loan -> loan.getMonthlyInstallment().min(loan.getRemainingBalance()))
                .orElse(BigDecimal.ZERO);

        BigDecimal grossEarnings = fixedGross.add(overtimePayment).add(otherEarnings);
        BigDecimal totalDeductions = absentDeduction.add(tax).add(providentFund).add(otherDeductions).add(loanDue);

        return row
                .basic(basic).houseRent(houseRent).medical(medical)
                .transport(transport).food(food).special(special)
                .overtimeHours(overtimeHours).overtimePayment(overtimePayment)
                .otherEarnings(otherEarnings).otherDeductions(otherDeductions)
                .grossEarnings(grossEarnings)
                .absentDays(absentDays).absentDeduction(absentDeduction)
                .tax(tax).providentFund(providentFund)
                .totalDeductions(totalDeductions)
                .netPayable(grossEarnings.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private SalarySheetResponse totalled(List<SalarySheetRow> rows, int payMonth, int payYear,
                                         PayrollSettings settings, int divisor) {
        BigDecimal basic = BigDecimal.ZERO, rent = BigDecimal.ZERO, medical = BigDecimal.ZERO;
        BigDecimal transport = BigDecimal.ZERO, food = BigDecimal.ZERO, special = BigDecimal.ZERO;
        BigDecimal otHours = BigDecimal.ZERO, otPay = BigDecimal.ZERO, gross = BigDecimal.ZERO;
        BigDecimal absentAmt = BigDecimal.ZERO, tax = BigDecimal.ZERO, pf = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO, net = BigDecimal.ZERO;
        BigDecimal bonus = BigDecimal.ZERO, otherEarn = BigDecimal.ZERO, otherDed = BigDecimal.ZERO;
        int absentDays = 0;

        for (SalarySheetRow r : rows) {
            bonus = bonus.add(orZero(r.getBonus()));
            otherEarn = otherEarn.add(orZero(r.getOtherEarnings()));
            otherDed = otherDed.add(orZero(r.getOtherDeductions()));
            basic = basic.add(r.getBasic());
            rent = rent.add(r.getHouseRent());
            medical = medical.add(r.getMedical());
            transport = transport.add(r.getTransport());
            food = food.add(r.getFood());
            special = special.add(r.getSpecial());
            otHours = otHours.add(r.getOvertimeHours());
            otPay = otPay.add(r.getOvertimePayment());
            gross = gross.add(r.getGrossEarnings());
            absentDays += r.getAbsentDays();
            absentAmt = absentAmt.add(r.getAbsentDeduction());
            tax = tax.add(r.getTax());
            pf = pf.add(r.getProvidentFund());
            deductions = deductions.add(r.getTotalDeductions());
            net = net.add(r.getNetPayable());
        }

        return SalarySheetResponse.builder()
                .payMonth(payMonth).payYear(payYear)
                .perDayBasis(settings.getPerDayBasis().name())
                .perDayDivisor(divisor)
                .overtimeEnabled(settings.isOvertimeEnabled())
                .overtimeMultiplier(settings.getOvertimeMultiplier())
                .rows(rows)
                .totalBasic(basic).totalHouseRent(rent).totalMedical(medical)
                .totalTransport(transport).totalFood(food).totalSpecial(special)
                .totalOvertimeHours(otHours).totalOvertimePayment(otPay)
                .totalBonus(bonus).totalOtherEarnings(otherEarn).totalOtherDeductions(otherDed)
                .totalGrossEarnings(gross)
                .totalAbsentDays(absentDays).totalAbsentDeduction(absentAmt)
                .totalTax(tax).totalProvidentFund(pf)
                .totalDeductions(deductions).totalNetPayable(net)
                .build();
    }

    private String displayName(Employee e) {
        if (e.getUser() != null) {
            String name = ((e.getUser().getFirstName() == null ? "" : e.getUser().getFirstName()) + " "
                    + (e.getUser().getLastName() == null ? "" : e.getUser().getLastName())).trim();
            if (!name.isEmpty()) return name;
        }
        return e.getEmployeeNumber() != null ? e.getEmployeeNumber() : ("Employee #" + e.getId());
    }

    private String position(Employee e) {
        if (e.getDesignation() != null && e.getDesignation().getName() != null) {
            return e.getDesignation().getName();
        }
        return e.getJobTitle();
    }

    private BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
