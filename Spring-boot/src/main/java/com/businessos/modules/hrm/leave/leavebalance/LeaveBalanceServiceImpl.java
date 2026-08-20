package com.businessos.modules.hrm.leave.leavebalance;

import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaveBalanceServiceImpl implements LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository     employeeRepository;
    private final SecurityUtil           securityUtil;
    private final AuthorizationService   authorizationService;

    @Override
    @Transactional
    public LeaveBalanceResponse create(LeaveBalanceRequest request) {
        authorizationService.checkPermission(PermissionCode.LEAVE_BALANCE_CREATE);
        Long companyId = requireCompanyId();
        Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));

        leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
                employee.getId(), request.getLeaveType(), request.getYear())
            .ifPresent(existing -> {
                throw new BadRequestException(
                    "A " + request.getLeaveType() + " balance for " + request.getYear() + " already exists for this employee");
            });

        LeaveBalance balance = LeaveBalance.builder()
            .employee(employee)
            .company(employee.getCompany())
            .leaveType(request.getLeaveType())
            .year(request.getYear())
            .totalDays(request.getTotalDays())
            .build();

        leaveBalanceRepository.save(balance);
        return LeaveBalanceMapper.toLeaveBalanceResponse(balance);
    }

    @Override
    @Transactional
    public LeaveBalanceResponse update(Long id, LeaveBalanceRequest request) {
        authorizationService.checkPermission(PermissionCode.LEAVE_BALANCE_UPDATE);
        Long companyId = requireCompanyId();
        LeaveBalance balance = findInTenant(id, companyId);

        if (request.getEmployeeId() != null && !request.getEmployeeId().equals(balance.getEmployee().getId())) {
            Employee employee = employeeRepository.findByIdAndCompanyId(request.getEmployeeId(), companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + request.getEmployeeId()));
            balance.setEmployee(employee);
        }
        balance.setLeaveType(request.getLeaveType());
        balance.setYear(request.getYear());
        balance.setTotalDays(request.getTotalDays());

        return LeaveBalanceMapper.toLeaveBalanceResponse(balance);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.LEAVE_BALANCE_DELETE);
        Long companyId = requireCompanyId();
        findInTenant(id, companyId).softDelete();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveBalanceResponse> listAll(int year, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.LEAVE_BALANCE_VIEW);
        Long companyId = requireCompanyId();
        return leaveBalanceRepository.findByCompanyIdAndYear(companyId, year, pageable)
            .map(LeaveBalanceMapper::toLeaveBalanceResponse);
    }

    /**
     * The caller's own balances. Deliberately NOT gated on
     * LEAVE_BALANCE_VIEW: that permission governs seeing OTHER people's
     * balances, and an employee must always be able to see their own.
     * Scoping is by the caller's own employee record, so there is nothing
     * here they could reach that isn't theirs.
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.List<LeaveBalanceResponse> listMine(int year) {
        Long companyId = requireCompanyId();
        Employee me = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
            .orElseThrow(() -> new BadRequestException("No employee profile for the current user"));

        // A user could in principle hold an employee record in more than one
        // tenant; make sure we're reading the one for the active company.
        if (me.getCompany() == null || !me.getCompany().getId().equals(companyId)) {
            throw new BadRequestException("Employee profile does not belong to the active company");
        }

        return leaveBalanceRepository.findByEmployeeIdAndYear(me.getId(), year)
            .stream().map(LeaveBalanceMapper::toLeaveBalanceResponse).toList();
    }

    private LeaveBalance findInTenant(Long id, Long companyId) {
        return leaveBalanceRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }
}
