package com.businessos.modules.hrm.payroll.run;

import com.businessos.shared.exception.BadRequestException;
import com.businessos.enums.PayrollStatus;
import com.businessos.enums.PaymentMethod;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.modules.hrm.payroll.Payroll;
import com.businessos.modules.hrm.payroll.PayrollRepository;
import com.businessos.modules.hrm.payroll.PayrollService;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static com.businessos.modules.hrm.payroll.run.PayrollRun.RunStatus.*;

/**
 * The payroll batch workflow. A run wraps the period's per-employee Payroll
 * rows: create generates/adopts lines, calculate refreshes totals, then
 * submit -> approve -> pay walks the spec's approval chain. Approving the run
 * approves every DRAFT line; paying it drives each line through the existing
 * markPaid flow so GL posting and payment references behave exactly like a
 * single payroll paid by hand.
 */
@Service
@RequiredArgsConstructor
public class PayrollRunService {

    private final PayrollRunRepository runRepository;
    private final PayrollRepository payrollRepository;
    private final PayrollService payrollService;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    public List<PayrollRun> list() {
        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);
        return runRepository.findByCompanyIdOrderByPayYearDescPayMonthDesc(securityUtil.getCurrentCompanyId());
    }

    public PayrollRun getForPeriod(int month, int year) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_VIEW);
        return runRepository.findByCompanyIdAndPayMonthAndPayYear(securityUtil.getCurrentCompanyId(), month, year)
                .orElse(null);
    }

    /** Creates the period's run: generates missing lines, adopts existing ones. */
    @Transactional
    public PayrollRun create(int month, int year, String remarks) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_PROCESS);
        Long companyId = securityUtil.getCurrentCompanyId();
        runRepository.findByCompanyIdAndPayMonthAndPayYear(companyId, month, year).ifPresent(r -> {
            throw new BadRequestException("A payroll run for " + month + "/" + year + " already exists (" + r.getRunNumber() + ")");
        });

        // Generate rows for everyone who has a structure and no payroll yet.
        payrollService.generateForAllEmployees(month, year);

        YearMonth ym = YearMonth.of(year, month);
        PayrollRun run = PayrollRun.builder()
                .company(companyRepository.getReferenceById(companyId))
                .runNumber("PR-" + year + String.format("%02d", month) + "-" + (runRepository.countByCompanyId(companyId) + 1))
                .payMonth(month).payYear(year)
                .payPeriodStart(ym.atDay(1))
                .payPeriodEnd(ym.atEndOfMonth())
                .remarks(remarks)
                .createdById(securityUtil.getCurrentUser() != null ? securityUtil.getCurrentUser().getId() : null)
                .status(DRAFT)
                .build();
        run = runRepository.save(run);

        // Adopt every line of the period that isn't already in another run.
        List<Payroll> lines = payrollRepository.findAllByCompanyIdAndPayMonthAndPayYear(companyId, month, year);
        for (Payroll p : lines) {
            if (p.getRun() == null) {
                p.setRun(run);
                payrollRepository.save(p);
            }
        }
        refreshTotals(run);
        run.setStatus(CALCULATED);
        return runRepository.save(run);
    }

    /** Re-adopts new lines and refreshes totals; allowed until approval. */
    @Transactional
    public PayrollRun recalculate(Long id) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_PROCESS);
        PayrollRun run = owned(id);
        requireStatus(run, "recalculate", DRAFT, CALCULATED, PENDING_APPROVAL, REJECTED);
        payrollService.generateForAllEmployees(run.getPayMonth(), run.getPayYear());
        for (Payroll p : payrollRepository.findAllByCompanyIdAndPayMonthAndPayYear(
                run.getCompany().getId(), run.getPayMonth(), run.getPayYear())) {
            if (p.getRun() == null) { p.setRun(run); payrollRepository.save(p); }
        }
        refreshTotals(run);
        if (run.getStatus() == DRAFT || run.getStatus() == REJECTED) run.setStatus(CALCULATED);
        return runRepository.save(run);
    }

    @Transactional
    public PayrollRun submit(Long id) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_PROCESS);
        PayrollRun run = owned(id);
        requireStatus(run, "submit", DRAFT, CALCULATED, REJECTED);
        if (payrollRepository.findByRunId(run.getId()).isEmpty())
            throw new BadRequestException("Run has no payroll lines - recalculate first");
        refreshTotals(run);
        run.setStatus(PENDING_APPROVAL);
        run.setRejectionReason(null);
        return runRepository.save(run);
    }

    /** Approves the run and every DRAFT line in it; lines lock from here on. */
    @Transactional
    public PayrollRun approve(Long id) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_APPROVE);
        PayrollRun run = owned(id);
        requireStatus(run, "approve", PENDING_APPROVAL, CALCULATED);
        for (Payroll p : payrollRepository.findByRunId(run.getId())) {
            if (p.getStatus() == PayrollStatus.DRAFT) payrollService.approve(p.getId());
        }
        refreshTotals(run);
        run.setStatus(APPROVED);
        run.setApprovedById(securityUtil.getCurrentUser() != null ? securityUtil.getCurrentUser().getId() : null);
        run.setApprovedAt(LocalDateTime.now());
        return runRepository.save(run);
    }

    @Transactional
    public PayrollRun reject(Long id, String reason) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_APPROVE);
        PayrollRun run = owned(id);
        requireStatus(run, "reject", PENDING_APPROVAL);
        run.setStatus(REJECTED);
        run.setRejectionReason(reason);
        return runRepository.save(run);
    }

    @Transactional
    public PayrollRun cancel(Long id) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_PROCESS);
        PayrollRun run = owned(id);
        requireStatus(run, "cancel", DRAFT, CALCULATED, PENDING_APPROVAL, REJECTED);
        run.setStatus(CANCELLED);
        return runRepository.save(run);
    }

    /** Pays every unpaid line through the existing single-payroll flow (GL and all). */
    @Transactional
    public PayrollRun pay(Long id, PaymentMethod method, String referencePrefix, LocalDate paymentDate) {
        authorizationService.checkPermission(PermissionCode.PAYROLL_APPROVE);
        PayrollRun run = owned(id);
        requireStatus(run, "pay", APPROVED);
        List<Payroll> lines = payrollRepository.findByRunId(run.getId());
        int seq = 1;
        for (Payroll p : lines) {
            if (p.getStatus() == PayrollStatus.PAID || p.getStatus() == PayrollStatus.CANCELLED) continue;
            String ref = (referencePrefix == null || referencePrefix.isBlank() ? run.getRunNumber() : referencePrefix)
                    + "-" + String.format("%03d", seq++);
            payrollService.markPaid(p.getId(), ref, method);
        }
        run.setPaymentDate(paymentDate != null ? paymentDate : LocalDate.now());
        run.setStatus(PAID);
        return runRepository.save(run);
    }

    private void refreshTotals(PayrollRun run) {
        List<Payroll> lines = payrollRepository.findByRunId(run.getId());
        BigDecimal gross = BigDecimal.ZERO, net = BigDecimal.ZERO;
        for (Payroll p : lines) {
            BigDecimal lineGross = nz(p.getBasicSalary()).add(nz(p.getHouseRent())).add(nz(p.getMedicalAllowance()))
                    .add(nz(p.getTransportAllowance())).add(nz(p.getFoodAllowance())).add(nz(p.getSpecialAllowance()))
                    .add(nz(p.getBonus())).add(nz(p.getBillablePay())).add(nz(p.getOvertimePay())).add(nz(p.getOtherEarnings()));
            gross = gross.add(lineGross);
            net = net.add(nz(p.getNetSalary()));
        }
        run.setTotalEmployees(lines.size());
        run.setTotalGross(gross);
        run.setTotalNet(net);
        run.setTotalDeduction(gross.subtract(net));
    }

    private PayrollRun owned(Long id) {
        PayrollRun run = runRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Payroll run not found"));
        if (!run.getCompany().getId().equals(securityUtil.getCurrentCompanyId()))
            throw new BadRequestException("Payroll run not found");
        return run;
    }

    private void requireStatus(PayrollRun run, String action, PayrollRun.RunStatus... allowed) {
        for (PayrollRun.RunStatus s : allowed) if (run.getStatus() == s) return;
        throw new BadRequestException("Cannot " + action + " a " + run.getStatus() + " payroll run");
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
