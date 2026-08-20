package com.businessos.modules.hrm.payroll.loan;

import com.businessos.modules.company.Company;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Employee loans/advances recovered automatically through payroll. Creation
 * and cancellation live here; the actual per-period installment computation
 * and PAID-time balance settlement live in PayrollServiceImpl, which is the
 * only place a payroll's status legitimately changes.
 */
@Service
@RequiredArgsConstructor
public class LoanAdvanceService {

    private final LoanAdvanceRepository loanRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public LoanAdvanceResponse create(CreateLoanRequest request) {
        Long companyId = requireCompanyId();
        Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        // One ACTIVE loan per employee at a time - keeps "how much comes out of
        // this month's pay" unambiguous instead of needing to stack/prioritize
        // multiple concurrent loans.
        if (loanRepository.existsByEmployeeIdAndStatus(employee.getId(), LoanAdvance.Status.ACTIVE)) {
            throw new BadRequestException(
                    employeeDisplayName(employee) + " already has an active loan/advance - close or cancel it first");
        }

        LoanAdvance loan = LoanAdvance.builder()
                .company(companyRef(companyId))
                .employee(employee)
                .type(request.getType())
                .principalAmount(request.getPrincipalAmount())
                .disbursedDate(request.getDisbursedDate())
                .monthlyInstallment(request.getMonthlyInstallment())
                .remainingBalance(request.getPrincipalAmount())
                .status(LoanAdvance.Status.ACTIVE)
                .reason(request.getReason())
                .notes(request.getNotes())
                .build();
        loanRepository.save(loan);
        return LoanAdvanceMapper.toResponse(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanAdvanceResponse> list() {
        return loanRepository.findByCompanyIdOrderByCreatedAtDesc(requireCompanyId())
                .stream().map(LoanAdvanceMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanAdvanceResponse> listForEmployee(Long employeeId) {
        Long companyId = requireCompanyId();
        employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        return loanRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream().map(LoanAdvanceMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LoanAdvanceResponse getById(Long id) {
        return LoanAdvanceMapper.toResponse(findInTenant(id));
    }

    @Transactional(readOnly = true)
    public List<LoanRepaymentResponse> repaymentHistory(Long loanId) {
        findInTenant(loanId); // tenant check
        return repaymentRepository.findByLoanIdOrderByPaidDateDesc(loanId)
                .stream().map(LoanAdvanceMapper::toResponse).toList();
    }

    /**
     * Only legal before any installment has actually been recovered - once
     * payroll has paid one, the loan is money already partly out the door and
     * has to run to completion (or be handled manually), not disappear.
     */
    @Transactional
    public LoanAdvanceResponse cancel(Long id) {
        LoanAdvance loan = findInTenant(id);
        if (loan.getStatus() != LoanAdvance.Status.ACTIVE) {
            throw new BadRequestException("Only an active loan can be cancelled");
        }
        if (!repaymentRepository.findByLoanIdOrderByPaidDateDesc(id).isEmpty()) {
            throw new BadRequestException("Cannot cancel: this loan already has recorded repayments");
        }
        loan.setStatus(LoanAdvance.Status.CANCELLED);
        loanRepository.save(loan);
        return LoanAdvanceMapper.toResponse(loan);
    }

    private LoanAdvance findInTenant(Long id) {
        return loanRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan/advance not found: " + id));
    }

    private String employeeDisplayName(Employee employee) {
        return employee.getUser() != null ? employee.getUser().getFullName() : "Employee #" + employee.getId();
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company();
        c.setId(companyId);
        return c;
    }
}
