package com.businessos.modules.hrm.payroll;

import com.businessos.modules.company.Company;
import com.businessos.modules.finance.chartofaccounts.ChartOfAccount;
import com.businessos.modules.finance.chartofaccounts.DefaultAccountResolver;
import com.businessos.modules.finance.generalledger.GeneralLedgerService;
import com.businessos.modules.finance.generalledger.GlReferenceType;
import com.businessos.modules.finance.generalledger.LedgerLine;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.attendance.attendance.AttendanceRepository;
import com.businessos.modules.hrm.attendance.attendance.AttendanceStatus;
import com.businessos.modules.hrm.salary.SalaryStructure;
import com.businessos.modules.hrm.salary.SalaryStructureRepository;
import com.businessos.enums.PayrollStatus;
import com.businessos.enums.PaymentMethod;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.email.EmailService;
import com.businessos.modules.company.CompanyRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final AttendanceRepository attendanceRepository;
    private final com.businessos.modules.hrm.attendance.timesheet.TimesheetRepository timesheetRepository;
    private final SecurityUtil securityUtil;
    private final EmailService emailService;
    private final EmailBranding emailBranding;
    private final com.businessos.shared.notification.NotificationService notificationService;
    private final GeneralLedgerService glService;
    private final DefaultAccountResolver accountResolver;
    private final com.businessos.modules.hrm.payroll.settings.PayrollSettingsService payrollSettingsService;
    private final PayslipPdfService payslipPdfService;
    private final com.businessos.auth.role.service.AuthorizationService authorizationService;
    private final com.businessos.modules.hrm.payroll.components.SalaryComponentService salaryComponentService;
    private final com.businessos.modules.hrm.leave.LeaveService leaveService;
    private final com.businessos.modules.hrm.payroll.loan.LoanAdvanceRepository loanAdvanceRepository;
    private final com.businessos.modules.hrm.payroll.loan.LoanRepaymentRepository loanRepaymentRepository;

    @Override
    @Transactional
    public PayrollResponse create(CreatePayrollRequest request) {
        Long companyId = requireCompanyId();

        if (payrollRepository.findByEmployeeIdAndPayMonthAndPayYear(
                request.getEmployeeId(), request.getPayMonth(), request.getPayYear()).isPresent()) {
            throw new BadRequestException("Payroll already exists for this employee and period");
        }

        Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        SalaryComponents comps = resolveSalaryComponents(employee, request.getPayMonth(), request.getPayYear(),
                request.getBasicSalary(), request.getHouseRent(), request.getMedicalAllowance(), request.getTransportAllowance(),
                request.getFoodAllowance(), request.getSpecialAllowance());

        BigDecimal bonus = orZero(request.getBonus());
        BigDecimal deductions = orZero(request.getDeductions());
        BigDecimal tax = orZero(request.getTaxDeduction());
        BigDecimal insurance = orZero(request.getInsuranceDeduction());
        BigDecimal providentFund = orZero(request.getProvidentFundDeduction());
        BigDecimal grossForAttendance = comps.basic().add(comps.rent()).add(comps.medical()).add(comps.transport())
                .add(comps.food()).add(comps.special());
        BillablePay billable = calculateBillablePay(employee, request.getPayMonth(), request.getPayYear());
        Overtime overtime = calculateOvertime(employee, request.getPayMonth(), request.getPayYear(),
                grossForAttendance, comps.basic());
        ExtraSums extras = extraComponentSums(employee.getId(), request.getPayMonth(), request.getPayYear());
        BigDecimal gross = grossForAttendance.add(bonus).add(billable.amount()).add(overtime.amount())
                .add(extras.earnings());
        AttendanceDeduction attendanceDeduction = calculateAttendanceDeduction(
                employee, request.getPayMonth(), request.getPayYear(), grossForAttendance, comps.basic());
        LoanDue loanDue = calculateLoanDue(employee);
        BigDecimal net = gross.subtract(deductions).subtract(tax).subtract(insurance).subtract(providentFund)
                .subtract(attendanceDeduction.amount()).subtract(extras.deductions()).subtract(loanDue.amount());

        Payroll payroll = Payroll.builder()
                .employee(employee)
                .company(companyRef(companyId))
                .payMonth(request.getPayMonth())
                .payYear(request.getPayYear())
                .basicSalary(comps.basic())
                .houseRent(comps.rent())
                .medicalAllowance(comps.medical())
                .transportAllowance(comps.transport())
                .foodAllowance(comps.food())
                .specialAllowance(comps.special())
                .bonus(bonus)
                .billableHours(billable.hours())
                .billableRate(billable.rate())
                .billablePay(billable.amount())
                .overtimeHours(overtime.hours())
                .overtimeRate(overtime.hourlyRate())
                .overtimePay(overtime.amount())
                .deductions(deductions)
                .taxDeduction(tax)
                .insuranceDeduction(insurance)
                .providentFundDeduction(providentFund)
                .attendanceDeduction(attendanceDeduction.amount())
                .absentDays(attendanceDeduction.days())
                .otherEarnings(extras.earnings())
                .otherDeductions(extras.deductions())
                .loanAdvance(loanDue.loan())
                .loanDeductionAmount(loanDue.amount())
                .netSalary(net)
                .notes(request.getNotes())
                .status(PayrollStatus.DRAFT)
                .build();

        payrollRepository.save(payroll);

        return PayrollMapper.toPayrollResponse(payroll);
    }

    @Override
    @Transactional
    public BulkPayrollResult generateForAllEmployees(int month, int year) {
        Long companyId = requireCompanyId();
        List<Employee> employees = employeeRepository.findByCompanyIdAndActiveTrue(companyId);

        List<String> created = new ArrayList<>();
        List<String> skippedAlreadyExists = new ArrayList<>();
        List<String> skippedNoStructure = new ArrayList<>();

        for (Employee employee : employees) {
            String name = employeeDisplayName(employee);

            if (payrollRepository.findByEmployeeIdAndPayMonthAndPayYear(employee.getId(), month, year).isPresent()) {
                skippedAlreadyExists.add(name);
                continue;
            }

            Optional<SalaryStructure> structureOpt = activeStructure(employee.getId(), month, year);
            if (structureOpt.isEmpty()) {
                skippedNoStructure.add(name);
                continue;
            }
            SalaryStructure structure = structureOpt.get();
            SalaryComponents comps = fromStructure(structure);
            BigDecimal tax = orZero(structure.getTaxDeduction());
            BigDecimal providentFund = orZero(structure.getProvidentFund());
            BigDecimal grossForAttendance = comps.basic().add(comps.rent()).add(comps.medical()).add(comps.transport())
                    .add(comps.food()).add(comps.special());
            BillablePay billable = calculateBillablePay(employee, month, year);
            Overtime overtime = calculateOvertime(employee, month, year, grossForAttendance, comps.basic());
            ExtraSums extras = new ExtraSums(
                    salaryComponentService.sumExtras(structure.getId(),
                            com.businessos.modules.hrm.payroll.components.SalaryComponent.ComponentType.EARNING),
                    salaryComponentService.sumExtras(structure.getId(),
                            com.businessos.modules.hrm.payroll.components.SalaryComponent.ComponentType.DEDUCTION));
            BigDecimal gross = grossForAttendance.add(billable.amount()).add(overtime.amount()).add(extras.earnings());
            AttendanceDeduction attendanceDeduction = calculateAttendanceDeduction(employee, month, year, grossForAttendance, comps.basic());
            LoanDue loanDue = calculateLoanDue(employee);
            BigDecimal net = gross.subtract(providentFund).subtract(tax).subtract(attendanceDeduction.amount())
                    .subtract(extras.deductions()).subtract(loanDue.amount());

            Payroll payroll = Payroll.builder()
                    .employee(employee)
                    .company(companyRef(companyId))
                    .payMonth(month)
                    .payYear(year)
                    .basicSalary(comps.basic())
                    .houseRent(comps.rent())
                    .medicalAllowance(comps.medical())
                    .transportAllowance(comps.transport())
                    .foodAllowance(comps.food())
                    .specialAllowance(comps.special())
                    .bonus(BigDecimal.ZERO)
                    .billableHours(billable.hours())
                    .billableRate(billable.rate())
                    .billablePay(billable.amount())
                    .overtimeHours(overtime.hours())
                    .overtimeRate(overtime.hourlyRate())
                    .overtimePay(overtime.amount())
                    .taxDeduction(tax)
                    .providentFundDeduction(providentFund)
                    .attendanceDeduction(attendanceDeduction.amount())
                    .absentDays(attendanceDeduction.days())
                    .otherEarnings(extras.earnings())
                    .otherDeductions(extras.deductions())
                    .loanAdvance(loanDue.loan())
                    .loanDeductionAmount(loanDue.amount())
                    .netSalary(net)
                    .status(PayrollStatus.DRAFT)
                    .build();
            payrollRepository.save(payroll);
            created.add(name);
        }

        return BulkPayrollResult.builder()
                .created(created)
                .skippedAlreadyExists(skippedAlreadyExists)
                .skippedNoSalaryStructure(skippedNoStructure)
                .build();
    }

    private record SalaryComponents(BigDecimal basic, BigDecimal rent, BigDecimal medical, BigDecimal transport,
            BigDecimal food, BigDecimal special) {}

    /** Frozen sums of the structure's extra catalog components. */
    private record ExtraSums(BigDecimal earnings, BigDecimal deductions) {}

    private ExtraSums extraComponentSums(Long employeeId, int month, int year) {
        return activeStructure(employeeId, month, year)
                .map(s -> new ExtraSums(
                        salaryComponentService.sumExtras(s.getId(),
                                com.businessos.modules.hrm.payroll.components.SalaryComponent.ComponentType.EARNING),
                        salaryComponentService.sumExtras(s.getId(),
                                com.businessos.modules.hrm.payroll.components.SalaryComponent.ComponentType.DEDUCTION)))
                .orElse(new ExtraSums(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private record AttendanceDeduction(BigDecimal amount, int days) {}

    /** loan is null when the employee has no active loan - amount is always non-null. */
    private record LoanDue(com.businessos.modules.hrm.payroll.loan.LoanAdvance loan, BigDecimal amount) {}

    /**
     * The installment due this period against the employee's one active loan,
     * capped at whatever balance remains so the final installment doesn't
     * overshoot. Never mutates the loan - remainingBalance only moves once
     * this payroll actually reaches PAID, see settleLoanInstallment below.
     */
    private LoanDue calculateLoanDue(Employee employee) {
        return loanAdvanceRepository
                .findFirstByEmployeeIdAndStatus(employee.getId(), com.businessos.modules.hrm.payroll.loan.LoanAdvance.Status.ACTIVE)
                .map(loan -> new LoanDue(loan, loan.getMonthlyInstallment().min(loan.getRemainingBalance())))
                .orElse(new LoanDue(null, BigDecimal.ZERO));
    }

    private record BillablePay(BigDecimal hours, BigDecimal rate, BigDecimal amount) {}

    /** hourlyRate already includes the overtime multiplier. */
    private record Overtime(BigDecimal hours, BigDecimal hourlyRate, BigDecimal amount) {}

    /**
     * Approved timesheet billableHours for the pay period * the employee's fixed
     * billableRate - added to gross/net on top of their salary. Unsubmitted/unapproved
     * hours don't count yet (see TimesheetRepository.sumApprovedBillableHours).
     */
    private BillablePay calculateBillablePay(Employee employee, int payMonth, int payYear) {
        BigDecimal rate = orZero(employee.getBillableRate());
        LocalDate start = LocalDate.of(payYear, payMonth, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        BigDecimal hours = BigDecimal.valueOf(
                timesheetRepository.sumApprovedBillableHours(employee.getId(), start, end).orElse(0.0));
        BigDecimal amount = rate.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : rate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        return new BillablePay(hours, rate, amount);
    }

    /**
     * PaymentMethod is shared app-wide (invoices, expenses, payroll), so it lists
     * collection rails that make no sense for paying an employee.
     *
     * SSLCOMMERZ moves money *to* the company from a payer's card or wallet - it
     * has no payout API, so recording a salary as "paid by SSLCommerz" describes
     * something that cannot have happened. WALLET is a company-level balance with
     * no per-employee counterparty. Both are rejected here as well as hidden in
     * the UI, so the API cannot be used to record an impossible payment.
     */
    /**
     * Bank disbursement file for a pay period.
     *
     * Deliberately only APPROVED rows: DRAFT has not been signed off, and PAID has
     * already been sent - exporting either invites paying someone twice or paying
     * an unapproved amount.
     *
     * This does NOT move money. It produces the sheet finance uploads to the
     * bank's corporate portal (BEFTN/RTGS bulk salary), after which they come
     * back and mark each row paid with the bank's reference.
     *
     * Employees with no bank account still appear, with a blank account column
     * and a flagged note - silently dropping them is how someone quietly goes
     * unpaid for a month.
     */
    @Override
    @Transactional(readOnly = true)
    public String buildDisbursementCsv(int month, int year) {
        // Permission is checked in PayrollController, matching how every other
        // method in this module does it.
        Long companyId = requireCompanyId();

        List<Payroll> rows = payrollRepository
            .findForDisbursement(companyId, month, year, PayrollStatus.APPROVED);

        StringBuilder csv = new StringBuilder();
        csv.append("Employee ID,Employee Name,Bank Name,Account Number,Routing Number,Net Salary,Currency,Reference,Note\n");

        String period = String.format("%04d-%02d", year, month);
        for (Payroll p : rows) {
            Employee e = p.getEmployee();
            String name = e != null && e.getUser() != null ? e.getUser().getFullName() : "";
            String account = e != null ? nullToEmpty(e.getBankAccountNumber()) : "";
            String note = account.isBlank() ? "MISSING BANK ACCOUNT - cannot transfer" : "";

            csv.append(csvCell(e != null ? e.getEmployeeNumber() : "")).append(',')
               .append(csvCell(name)).append(',')
               .append(csvCell(e != null ? e.getBankName() : "")).append(',')
               .append(csvCell(account)).append(',')
               .append(csvCell(e != null ? e.getBankRoutingNumber() : "")).append(',')
               .append(p.getNetSalary() != null ? p.getNetSalary().toPlainString() : "0.00").append(',')
               .append("BDT,")
               .append(csvCell("SALARY-" + period)).append(',')
               .append(csvCell(note)).append('\n');
        }

        return csv.toString();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Quotes a CSV cell. A leading =, +, - or @ is prefixed with a single quote:
     * spreadsheet software treats those as formulas, so an employee name like
     * "=cmd" would execute rather than display (CSV injection).
     */
    private String csvCell(String raw) {
        String v = nullToEmpty(raw);
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    private void guardPayoutMethod(PaymentMethod method) {
        if (method == PaymentMethod.SSLCOMMERZ || method == PaymentMethod.WALLET) {
            throw new BadRequestException(
                method.name() + " cannot be used to pay salary - it is a collection method, not a payout. "
                    + "Use BANK_TRANSFER, BKASH, NAGAD, ROCKET, CHEQUE or CASH.");
        }
    }

    /**
     * ABSENT days (unapproved - approved leave is a separate ON_LEAVE status and
     * never becomes ABSENT, see AbsenteeMarkingService) are deducted at
     * gross / calendar-days-in-month per day.
     */
    private AttendanceDeduction calculateAttendanceDeduction(Employee employee, int payMonth, int payYear,
                                                            BigDecimal gross, BigDecimal basic) {
        LocalDate start = LocalDate.of(payYear, payMonth, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        int absentDays = (int) attendanceRepository.countByEmployeeIdAndStatusAndAttendanceDateBetween(
                employee.getId(), AttendanceStatus.ABSENT, start, end);
        // Approved UNPAID leave is leave-without-pay: it never becomes ABSENT
        // in attendance, but it still reduces pay at the same per-day rate.
        absentDays += leaveService.unpaidLeaveDays(employee.getId(), payMonth, payYear);
        if (absentDays == 0) {
            return new AttendanceDeduction(BigDecimal.ZERO, absentDays);
        }

        Long companyId = employee.getCompany() != null
                ? employee.getCompany().getId()
                : securityUtil.getCurrentCompanyId();
        com.businessos.modules.hrm.payroll.settings.PayrollSettings settings =
                payrollSettingsService.getOrCreate(companyId);

        BigDecimal base = settings.getAbsenceDeductionBase() == com.businessos.enums.SalaryBase.BASIC ? basic : gross;
        if (base == null || base.compareTo(BigDecimal.ZERO) == 0) {
            return new AttendanceDeduction(BigDecimal.ZERO, absentDays);
        }

        BigDecimal perDayRate = payrollSettingsService.perDayRate(settings, base, payMonth, payYear);
        BigDecimal amount = perDayRate.multiply(BigDecimal.valueOf(absentDays)).setScale(2, RoundingMode.HALF_UP);
        return new AttendanceDeduction(amount, absentDays);
    }

    /**
     * Overtime for the period, priced from the company's payroll settings.
     *
     * Deliberately identical in method to SalarySheetService: same hours source
     * (attendance), same base (BASIC or GROSS per settings), same per-day
     * divisor, same multiplier. The salary sheet is the preview of a payroll
     * run, so if the two disagreed the preview would be a lie.
     *
     * Returns zero when overtime is switched off for the company, which is the
     * default - a company that has not opted in does not silently start paying
     * for hours logged against a shift.
     */
    private Overtime calculateOvertime(Employee employee, int payMonth, int payYear,
                                       BigDecimal gross, BigDecimal basic) {
        Long companyId = employee.getCompany() != null
                ? employee.getCompany().getId()
                : securityUtil.getCurrentCompanyId();
        com.businessos.modules.hrm.payroll.settings.PayrollSettings settings =
                payrollSettingsService.getOrCreate(companyId);

        if (!settings.isOvertimeEnabled()) {
            return new Overtime(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        LocalDate start = LocalDate.of(payYear, payMonth, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        BigDecimal hours = orZero(attendanceRepository.sumOvertimeHours(employee.getId(), start, end));
        if (hours.compareTo(BigDecimal.ZERO) <= 0) {
            return new Overtime(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal base = settings.getOvertimeBase() == com.businessos.enums.SalaryBase.GROSS ? gross : basic;
        BigDecimal hoursPerDay = orZero(settings.getStandardHoursPerDay());
        if (base.compareTo(BigDecimal.ZERO) <= 0 || hoursPerDay.compareTo(BigDecimal.ZERO) <= 0) {
            return new Overtime(hours, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal perDay = payrollSettingsService.perDayRate(settings, base, payMonth, payYear);
        BigDecimal hourly = perDay.divide(hoursPerDay, 4, RoundingMode.HALF_UP)
                .multiply(orZero(settings.getOvertimeMultiplier()));
        BigDecimal amount = hourly.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        return new Overtime(hours, hourly.setScale(4, RoundingMode.HALF_UP), amount);
    }

    private Optional<SalaryStructure> activeStructure(Long employeeId, int payMonth, int payYear) {
        return salaryStructureRepository.findActiveForEmployeeOnDate(employeeId, LocalDate.of(payYear, payMonth, 1));
    }

    private SalaryComponents fromStructure(SalaryStructure s) {
        return new SalaryComponents(orZero(s.getBasicSalary()), orZero(s.getHouseRent()),
                orZero(s.getMedicalAllowance()), orZero(s.getTransportAllowance()),
                orZero(s.getFoodAllowance()), orZero(s.getSpecialAllowance()));
    }

    /**
     * If basicSalary was provided manually, use the request's numbers as-is (matches
     * the previous behavior exactly). Otherwise pull from the employee's salary
     * structure active during this pay period - previously this lookup existed
     * (findActiveForEmployeeOnDate) but nothing ever called it, so HR had to hand-type
     * every salary component for every employee every month.
     */
    private SalaryComponents resolveSalaryComponents(Employee employee, int payMonth, int payYear,
            BigDecimal manualBasic, BigDecimal manualRent, BigDecimal manualMedical, BigDecimal manualTransport,
            BigDecimal manualFood, BigDecimal manualSpecial) {
        if (manualBasic != null) {
            return new SalaryComponents(manualBasic, orZero(manualRent), orZero(manualMedical), orZero(manualTransport),
                    orZero(manualFood), orZero(manualSpecial));
        }
        SalaryStructure structure = activeStructure(employee.getId(), payMonth, payYear)
                .orElseThrow(() -> new BadRequestException(
                        "No active salary structure for " + employeeDisplayName(employee) + " for " + payMonth + "/" + payYear
                                + " - set one up under Salary Structures, or provide basicSalary manually"));
        return fromStructure(structure);
    }

    private String employeeDisplayName(Employee employee) {
        return employee.getUser() != null ? employee.getUser().getFullName() : "Employee #" + employee.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollResponse getById(Long id) {
        return PayrollMapper.toPayrollResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollResponse> listByPeriod(int month, int year, Pageable pageable) {
        return payrollRepository.findByCompanyIdAndPayMonthAndPayYear(
                requireCompanyId(), month, year, pageable)
                .map(PayrollMapper::toPayrollResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollResponse> listForEmployee(Long employeeId, Pageable pageable) {
        return payrollRepository.findByCompanyIdAndEmployeeId(requireCompanyId(), employeeId, pageable)
                .map(PayrollMapper::toPayrollResponse);
    }

    @Override
    @Transactional
    public PayrollResponse approve(Long id) {
        Payroll p = findInTenant(id);
        if (p.getStatus() != PayrollStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT payrolls can be approved");
        }
        Employee approver = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
                .orElseThrow(() -> new BadRequestException("Employee profile not found"));
        p.setStatus(PayrollStatus.APPROVED);
        p.setApprovedBy(approver);
        payrollRepository.save(p); // explicit save for clarity and transaction safety
        return PayrollMapper.toPayrollResponse(p);
    }

    @Override
    @Transactional
    public PayrollResponse markPaid(Long id, String paymentReference, PaymentMethod paymentMethod) {
        Payroll p = findInTenant(id);
        if (p.getStatus() != PayrollStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED payrolls can be marked as paid");
        }
        guardPayoutMethod(paymentMethod);
        p.setStatus(PayrollStatus.PAID);
        p.setPaymentReference(paymentReference);
        p.setPaymentMethod(paymentMethod);
        p.setPaidAt(LocalDate.now());

        postPayrollToLedger(p);
        settleLoanInstallment(p);

        if (p.getEmployee().getUser() != null) {
            try {
                EmailBranding.Data branding = emailBranding.from(p.getCompany());
                emailService.sendPayrollEmail(
                        p.getEmployee().getUser().getEmail(),
                        p.getEmployee().getUser().getFirstName(), branding);
            } catch (Exception ex) {
                log.warn("Payroll email failed for employee {}: {}", p.getEmployee().getUser().getEmail(), ex.getMessage());
            }

            // Email-only with silent failure meant an SMTP outage or a bounced
            // address left the employee with no way to learn their payslip was
            // ready - no notification-center record existed as a backup. This
            // is independent of the email try/catch above: it must land even
            // when the email fails.
            notificationService.send(com.businessos.shared.notification.CreateNotificationRequest.of(
                    com.businessos.enums.NotificationType.PAYSLIP_READY,
                    "Payslip ready",
                    "Your payslip for this pay period has been processed and is ready to view.",
                    "/payroll/my-payslips",
                    p.getEmployee().getUser().getId(),
                    p.getCompany().getId()));
        }

        return PayrollMapper.toPayrollResponse(p);
    }

    /**
     * Payroll disbursement previously never touched the ledger at all - salary,
     * often a company's single biggest expense, was completely invisible to
     * Finance reports. Dr Salaries and Wages (gross) / Cr Cash (net paid out) /
     * Cr Payroll Payable (tax + deductions withheld but not yet remitted).
     */
    private void postPayrollToLedger(Payroll p) {
        Long companyId = p.getCompany().getId();
        // Expense recognized excludes unpaid-absence days - the company never incurred
        // that cost - so the ledger debit matches what's actually credited below.
        // Overtime and structure-component earnings are part of net pay, so they
        // must be part of the expense debit - omitting them fails the balance
        // guard the moment either is non-zero.
        BigDecimal gross = p.getBasicSalary().add(p.getHouseRent()).add(p.getMedicalAllowance())
                .add(p.getTransportAllowance()).add(p.getFoodAllowance()).add(p.getSpecialAllowance()).add(p.getBonus())
                .add(orZero(p.getBillablePay()))
                .add(orZero(p.getOvertimePay()))
                .add(orZero(p.getOtherEarnings()))
                .subtract(orZero(p.getAttendanceDeduction()));
        // Payroll Payable is "gross minus net cash, for reasons other than an
        // expense reduction" - a loan installment fits that exactly (the
        // company recovers it, it isn't remitted anywhere, but it still isn't
        // cash out the door), so it folds in here rather than needing its own
        // GL account. This is the existing payroll->ledger posting staying
        // correct, not a new GL entry for the loan feature itself.
        BigDecimal withheld = p.getDeductions().add(p.getTaxDeduction())
                .add(p.getInsuranceDeduction()).add(p.getProvidentFundDeduction())
                .add(orZero(p.getOtherDeductions())).add(orZero(p.getLoanDeductionAmount()));
        String description = "Payroll " + p.getPayMonth() + "/" + p.getPayYear()
                + " for " + p.getEmployee().getUser().getFullName();

        ChartOfAccount salaryExpense = accountResolver.salaryExpense(companyId);
        ChartOfAccount cash = accountResolver.cash(companyId);

        List<LedgerLine> lines = new ArrayList<>();
        lines.add(LedgerLine.debit(salaryExpense.getId(), gross));
        lines.add(LedgerLine.credit(cash.getId(), p.getNetSalary()));
        if (withheld.compareTo(BigDecimal.ZERO) > 0) {
            ChartOfAccount payable = accountResolver.payrollPayable(companyId);
            lines.add(LedgerLine.credit(payable.getId(), withheld));
        }
        glService.recordBalancedTransaction(companyId, lines, description,
                GlReferenceType.PAYROLL, p.getId(), p.getPaymentReference(), LocalDate.now());

        p.setGlDebitAccount(salaryExpense.getAccountCode());
        p.setGlCreditAccount(cash.getAccountCode());
    }

    /**
     * Recovers this payroll's frozen loanDeductionAmount against the loan's
     * remainingBalance and logs a LoanRepayment row. Deliberately HR-side
     * bookkeeping only - the money already left in postPayrollToLedger's net
     * pay figure, so this does not touch the GL. Per the architecture split
     * the user asked for: Payroll generates the data, Accounting already
     * consumed it above; this step is Payroll's own record-keeping.
     */
    private void settleLoanInstallment(Payroll p) {
        com.businessos.modules.hrm.payroll.loan.LoanAdvance loan = p.getLoanAdvance();
        BigDecimal due = p.getLoanDeductionAmount();
        if (loan == null || due == null || due.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        // A loan can be cancelled any time before its first repayment - exactly
        // the window a DRAFT/APPROVED payroll referencing it can still exist in.
        // Without this check, approving and paying a stale payroll would still
        // deduct and settle a repayment against a loan that's officially
        // cancelled. The deduction already left the employee's net pay
        // (postPayrollToLedger, above) - that's a payroll-approval bug to catch
        // earlier, not something this bookkeeping step can undo - but it must at
        // least not log a repayment against a dead loan.
        if (loan.getStatus() == com.businessos.modules.hrm.payroll.loan.LoanAdvance.Status.CANCELLED) {
            return;
        }
        BigDecimal newBalance = loan.getRemainingBalance().subtract(due).max(BigDecimal.ZERO);
        loan.setRemainingBalance(newBalance);
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(com.businessos.modules.hrm.payroll.loan.LoanAdvance.Status.CLOSED);
        }
        loanAdvanceRepository.save(loan);

        loanRepaymentRepository.save(com.businessos.modules.hrm.payroll.loan.LoanRepayment.builder()
                .loan(loan)
                .payroll(p)
                .amount(due)
                .paidDate(p.getPaidAt())
                .balanceAfter(newBalance)
                .build());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Payroll p = findInTenant(id);
        if (p.getStatus() == PayrollStatus.PAID) {
            throw new BadRequestException("Cannot delete a paid payroll");
        }
        // Payroll lock: once the batch is approved (or further along), its
        // lines are frozen - the spec's "lock payroll after approval".
        if (p.getRun() != null) {
            var rs = p.getRun().getStatus();
            if (rs == com.businessos.modules.hrm.payroll.run.PayrollRun.RunStatus.APPROVED
                    || rs == com.businessos.modules.hrm.payroll.run.PayrollRun.RunStatus.PAID) {
                throw new BadRequestException("This payroll belongs to an " + rs + " run and is locked");
            }
        }
        p.softDelete();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public PayslipDocument generatePayslipPdf(Long id) {
        Payroll payroll = findInTenant(id);

        // Same rule as getById: PAYROLL_VIEW sees anyone's payslip, everyone
        // else only their own. Enforced here rather than in the controller so a
        // second caller cannot reach the PDF without the check.
        if (!authorizationService.hasPermission(
                com.businessos.auth.role.enums.PermissionCode.PAYROLL_VIEW)) {
            var currentUser = securityUtil.getCurrentUser();
            Employee mine = currentUser != null
                    ? employeeRepository.findByUserId(currentUser.getId()).orElse(null)
                    : null;
            Long ownerId = payroll.getEmployee() != null ? payroll.getEmployee().getId() : null;
            if (mine == null || ownerId == null || !mine.getId().equals(ownerId)) {
                throw new com.businessos.shared.exception.ForbiddenException(
                        "Access denied: you can only download your own payslip");
            }
        }

        // A DRAFT has not been approved by anyone, so handing it out as a
        // document would give an employee a payslip for figures that can still
        // change underneath them.
        if (payroll.getStatus() == PayrollStatus.DRAFT
                && !authorizationService.hasPermission(
                        com.businessos.auth.role.enums.PermissionCode.PAYROLL_VIEW)) {
            throw new BadRequestException("This payslip is not available yet - it has not been approved.");
        }

        Company company = payroll.getCompany();
        EmailBranding.Data branding = emailBranding.from(company);
        byte[] pdf = payslipPdfService.generate(payroll, company, branding);
        return new PayslipDocument(pdf, payslipPdfService.fileName(payroll));
    }

    private Payroll findInTenant(Long id) {
        return payrollRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null)
            throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }

    private BigDecimal orZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
