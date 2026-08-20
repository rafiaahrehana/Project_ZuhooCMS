package com.businessos.modules.hrm.payroll.dashboard;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.enums.PayrollStatus;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.payroll.Payroll;
import com.businessos.modules.hrm.payroll.PayrollRepository;
import com.businessos.modules.hrm.payroll.run.PayrollRun;
import com.businessos.modules.hrm.payroll.run.PayrollRunRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * The payroll module's landing view: this month's cost, who's been paid,
 * where the run stands, and a six-month net-payroll trend. Read-only
 * aggregation over data other endpoints own.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/payroll-dashboard")
@PreAuthorize("hasAnyRole('COMPANY_OWNER', 'EMPLOYEE')")
public class PayrollDashboardController {

    private final PayrollRepository payrollRepository;
    private final PayrollRunRepository runRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<DashboardView> get(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);
        Long companyId = requireCompanyId();
        YearMonth period = (month != null && year != null)
            ? YearMonth.of(year, month) : YearMonth.now();

        List<Payroll> payrolls = payrollRepository
            .findByCompanyIdAndPayMonthAndPayYear(companyId, period.getMonthValue(), period.getYear(), PageRequest.of(0, 2000))
            .getContent();

        DashboardView view = new DashboardView();
        view.month = period.getMonthValue();
        view.year = period.getYear();
        view.totalEmployees = employeeRepository.countByCompanyId(companyId);
        view.payrollCount = payrolls.size();
        view.employeesPaid = payrolls.stream().filter(p -> p.getStatus() == PayrollStatus.PAID).count();
        view.pendingCount = payrolls.stream()
            .filter(p -> p.getStatus() == PayrollStatus.DRAFT || p.getStatus() == PayrollStatus.APPROVED).count();

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (Payroll p : payrolls) {
            gross = gross.add(grossOf(p));
            net = net.add(nz(p.getNetSalary()));
        }
        view.totalGross = gross;
        view.totalNet = net;
        view.totalDeductions = gross.subtract(net);

        runRepository.findByCompanyIdAndPayMonthAndPayYear(companyId, period.getMonthValue(), period.getYear())
            .ifPresent(run -> {
                view.runStatus = run.getStatus() != null ? run.getStatus().name() : null;
                view.runNumber = run.getRunNumber();
            });

        // Convention: salaries pay on the last day of the period.
        view.nextPayDate = period.atEndOfMonth();

        // Six months ending at the selected period - PAID money only, so the
        // trend shows what actually left the company.
        List<TrendPoint> trend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth m = period.minusMonths(i);
            BigDecimal paid = payrollRepository.sumNetSalaryByCompanyAndPeriod(
                    companyId, m.getMonthValue(), m.getYear(), PayrollStatus.PAID)
                .orElse(BigDecimal.ZERO);
            TrendPoint point = new TrendPoint();
            point.month = m.getMonthValue();
            point.year = m.getYear();
            point.netPaid = paid;
            trend.add(point);
        }
        view.trend = trend;
        return ResponseEntity.ok(view);
    }

    /** Same earnings arithmetic the payslip uses - gross is derived, never stored. */
    private BigDecimal grossOf(Payroll p) {
        return nz(p.getBasicSalary()).add(nz(p.getHouseRent())).add(nz(p.getMedicalAllowance()))
            .add(nz(p.getTransportAllowance())).add(nz(p.getFoodAllowance())).add(nz(p.getSpecialAllowance()))
            .add(nz(p.getBonus())).add(nz(p.getBillablePay())).add(nz(p.getOvertimePay()))
            .add(nz(p.getOtherEarnings()));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    @Getter @Setter
    public static class DashboardView {
        private int month;
        private int year;
        private long totalEmployees;
        private long payrollCount;
        private long employeesPaid;
        private long pendingCount;
        private BigDecimal totalGross;
        private BigDecimal totalNet;
        private BigDecimal totalDeductions;
        private String runStatus;
        private String runNumber;
        private LocalDate nextPayDate;
        private List<TrendPoint> trend;
    }

    @Getter @Setter
    public static class TrendPoint {
        private int month;
        private int year;
        private BigDecimal netPaid;
    }
}
