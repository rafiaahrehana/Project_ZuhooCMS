package com.businessos.modules.hrm.leave;

import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.modules.hrm.leave.leavebalance.LeaveBalance;
import com.businessos.modules.hrm.leave.leavebalance.LeaveBalanceMapper;
import com.businessos.modules.hrm.leave.leavebalance.LeaveBalanceRepository;
import com.businessos.modules.hrm.leave.leavebalance.LeaveBalanceResponse;
import com.businessos.modules.hrm.leave.leaverequest.*;
import com.businessos.modules.hrm.leave.companyleavePolicy.CompanyLeavePolicyRepository;
import com.businessos.modules.company.Company;
import com.businessos.modules.company.CompanyLeavePolicy;
import com.businessos.modules.hrm.employee.Employee;
import com.businessos.auth.user.User;
import com.businessos.enums.LeaveRequestStatus;
import java.util.List;

import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.email.EmailBranding;
import com.businessos.shared.email.EmailService;
import com.businessos.modules.company.CompanyRepository;
import com.businessos.security.SecurityUtil;
import com.businessos.enums.NotificationType;
import com.businessos.shared.notification.CreateNotificationRequest;
import com.businessos.shared.notification.NotificationService;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final SecurityUtil securityUtil;
    private final EmailService emailService;
    private final EmailBranding emailBranding;
    private final com.businessos.modules.hrm.leave.holiday.HolidayRepository holidayRepository;
    private final CompanyLeavePolicyRepository leavePolicyRepository;
    private final NotificationService notificationService;
    private final AuthorizationService authorizationService;
    private final com.businessos.modules.hrm.attendance.attendance.AttendanceRepository attendanceRepository;
    private final com.businessos.modules.hrm.attendance.shift.EmployeeShiftAssignmentRepository employeeShiftAssignmentRepository;

    @Override
    @Transactional
    public LeaveRequestResponse apply(LeaveRequestDto request) {
        Long companyId = requireCompanyId();
        User currentUser = securityUtil.getCurrentUser();
        Employee employee = employeeRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new BadRequestException("Employee profile not found"));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date must be on or after start date");
        }
        // Previously unblocked entirely: an employee already marked ABSENT for a
        // past date could submit-and-get-approved a leave request for those exact
        // dates, reclassifying an unexcused absence after the fact right before
        // payroll runs. HR/managers (LEAVE_APPROVE) can still backdate deliberately
        // - e.g. logging a sick day an employee only reported after the fact.
        if (request.getStartDate().isBefore(java.time.LocalDate.now())
                && !authorizationService.hasPermission(PermissionCode.LEAVE_APPROVE)) {
            throw new BadRequestException(
                    "Leave requests can't start in the past - ask HR to log it on your behalf if this is a retroactive request");
        }

        if (leaveRequestRepository.hasOverlappingLeave(
                employee.getId(), request.getStartDate(), request.getEndDate(),
                List.of(LeaveRequestStatus.REJECTED, LeaveRequestStatus.CANCELLED))) {
            throw new BadRequestException("You already have a leave request overlapping this period");
        }

        // Chargeable days exclude the employee's weekly off days and company
        // holidays - a Thursday-to-Sunday request over a FRI/SAT weekend
        // charges 2 days of balance, not 4.
        int totalDays = countChargeableDays(employee, companyId, request.getStartDate(), request.getEndDate());
        if (totalDays == 0) {
            throw new BadRequestException(
                    "The selected period contains only weekends and holidays - no leave balance is needed");
        }

        // maxConsecutiveDays is configurable in the UI but was never enforced -
        // a "max 10 consecutive days" policy didn't stop a 60-day single
        // request. Non-blocking lookup: no applicable policy just means no cap
        // to enforce here (provisionBalanceFromPolicy below still requires one
        // to exist for paid types).
        leavePolicyRepository.findApplicablePolicy(companyId, request.getLeaveType(), employee.getEmploymentType())
                .map(CompanyLeavePolicy::getMaxConsecutiveDays)
                .filter(max -> max != null && max > 0 && totalDays > max)
                .ifPresent(max -> {
                    throw new BadRequestException(
                            "This request spans " + totalDays + " day(s), exceeding the " + max
                                    + "-day consecutive limit for " + request.getLeaveType() + " leave.");
                });

        // Check leave balance - skipped only for UNPAID, which by definition
        // isn't drawn from any entitlement. Previously the check for every
        // OTHER type silently did nothing when no LeaveBalance row existed -
        // the default for any new hire nobody has manually provisioned - so
        // unlimited paid leave went completely unenforced. Now a missing row
        // is auto-provisioned from the company's leave policy (if one exists
        // for this leave type + the employee's employment type) rather than
        // silently skipped; with no applicable policy either, the request is
        // rejected until HR sets one up.
        if (request.getLeaveType() != com.businessos.enums.LeaveType.UNPAID) {
            LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
                    employee.getId(), request.getLeaveType(), request.getStartDate().getYear())
                    .orElseGet(() -> provisionBalanceFromPolicy(employee, companyId, request.getLeaveType(),
                            request.getStartDate().getYear()));

            if (balance.getRemainingDays() < totalDays) {
                throw new BadRequestException("Insufficient " + request.getLeaveType()
                        + " leave balance. Available: " + balance.getRemainingDays()
                        + " days, Requested: " + totalDays + " days.");
            }
            balance.setPendingDays(balance.getPendingDays() + totalDays);
        }

        LeaveRequest lr = LeaveRequest.builder()
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(totalDays)
                .reason(request.getReason())
                .status(LeaveRequestStatus.PENDING)
                .employee(employee)
                .company(companyRef(companyId))
                .build();

        leaveRequestRepository.save(lr);
        notifyApprover(lr, employee, companyId);
        return LeaveRequestMapper.toLeaveRequestResponse(lr);
    }

    /**
     * Previously nobody was told a leave request needed action - a manager
     * only found out by opening the queue themselves, so requests could sit
     * PENDING indefinitely with no one prompted to act. Notifies the
     * employee's reporting manager if they have one, else the company owner.
     */
    private void notifyApprover(LeaveRequest lr, Employee employee, Long companyId) {
        Employee manager = employee.getReportingManager();
        User recipient = manager != null ? manager.getUser() : null;
        if (recipient == null) {
            Company company = companyRepository.findById(companyId).orElse(null);
            recipient = company != null ? company.getOwner() : null;
        }
        if (recipient == null) return;

        notificationService.send(CreateNotificationRequest.of(
                NotificationType.LEAVE_REQUESTED,
                "Leave request awaiting review",
                (employee.getUser() != null ? employee.getUser().getFullName() : "An employee")
                        + " requested " + lr.getTotalDays() + " day(s) of " + lr.getLeaveType()
                        + " leave (" + lr.getStartDate() + " to " + lr.getEndDate() + ")",
                "/leaves",
                recipient.getId(),
                companyId));
    }

    /**
     * Creates the employee's LeaveBalance row for this type/year the first
     * time it's needed, sized from the company's applicable leave policy
     * (matched on leave type + the employee's employment type, company-wide
     * policies with no employment type preferred over one). No applicable
     * policy means the company hasn't set one up for this leave type -
     * refuse the request rather than letting it through unbounded.
     */
    private LeaveBalance provisionBalanceFromPolicy(Employee employee, Long companyId,
                                                     com.businessos.enums.LeaveType leaveType, int year) {
        CompanyLeavePolicy policy = leavePolicyRepository
                .findApplicablePolicy(companyId, leaveType, employee.getEmploymentType())
                .orElseThrow(() -> new BadRequestException(
                        "No " + leaveType + " leave policy is configured for this company - "
                                + "set one up under Leave Policies before this leave type can be requested"));

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .company(companyRef(companyId))
                .leaveType(leaveType)
                .year(year)
                .totalDays(policy.getAnnualEntitlement())
                .build();
        return leaveBalanceRepository.save(balance);
    }

    /**
     * Working days in [start, end]: skips the employee's weekly off days
     * (from their shift, defaulting to FRI/SAT like the Shift entity) and
     * company holidays.
     */
    private int countChargeableDays(Employee employee, Long companyId,
                                    java.time.LocalDate start, java.time.LocalDate end) {
        java.util.Set<java.time.DayOfWeek> offDays = weeklyOffDays(employee);
        java.util.Set<java.time.LocalDate> holidays = new java.util.HashSet<>();
        holidayRepository.findByCompanyAndDateRange(companyId, start, end)
                .forEach(h -> holidays.add(h.getDate()));

        int days = 0;
        for (java.time.LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (offDays.contains(d.getDayOfWeek())) continue;
            if (holidays.contains(d)) continue;
            days++;
        }
        return days;
    }

    @Override
    @Transactional(readOnly = true)
    public int unpaidLeaveDays(Long employeeId, int month, int year) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null || employee.getCompany() == null) return 0;
        java.time.LocalDate monthStart = java.time.LocalDate.of(year, month, 1);
        java.time.LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        int days = 0;
        for (var lr : leaveRequestRepository.findApprovedOverlapping(
                employeeId, com.businessos.enums.LeaveType.UNPAID, monthStart, monthEnd)) {
            java.time.LocalDate from = lr.getStartDate().isBefore(monthStart) ? monthStart : lr.getStartDate();
            java.time.LocalDate to = lr.getEndDate().isAfter(monthEnd) ? monthEnd : lr.getEndDate();
            days += countChargeableDays(employee, employee.getCompany().getId(), from, to);
        }
        return days;
    }

    private java.util.Set<java.time.DayOfWeek> weeklyOffDays(Employee employee) {
        // Read the employee's shift from EmployeeShiftAssignment, the same source
        // AttendanceServiceImpl/AbsenteeMarkingService use for late detection and
        // weekly-off calculation - Employee.shift is a second, unsynced field that
        // can silently drift from the shift roster.
        String csv = employeeShiftAssignmentRepository
                .findByCompanyIdAndEmployeeIdAndActive(employee.getCompany().getId(), employee.getId())
                .map(esa -> esa.getShift().getWeeklyOffDays())
                .orElse(null);
        if (csv == null || csv.isBlank()) csv = "FRI,SAT";
        java.util.Set<java.time.DayOfWeek> out = new java.util.HashSet<>();
        for (String token : csv.split(",")) {
            switch (token.trim().toUpperCase()) {
                case "MON" -> out.add(java.time.DayOfWeek.MONDAY);
                case "TUE" -> out.add(java.time.DayOfWeek.TUESDAY);
                case "WED" -> out.add(java.time.DayOfWeek.WEDNESDAY);
                case "THU" -> out.add(java.time.DayOfWeek.THURSDAY);
                case "FRI" -> out.add(java.time.DayOfWeek.FRIDAY);
                case "SAT" -> out.add(java.time.DayOfWeek.SATURDAY);
                case "SUN" -> out.add(java.time.DayOfWeek.SUNDAY);
                default -> { /* unknown token - ignore */ }
            }
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestResponse getById(Long id) {
        return LeaveRequestMapper.toLeaveRequestResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> listAll(LeaveRequestStatus status, Pageable pageable) {
        Long companyId = requireCompanyId();
        Page<LeaveRequest> page = status != null
                ? leaveRequestRepository.findByCompanyIdAndStatus(companyId, status, pageable)
                : leaveRequestRepository.findByCompanyId(companyId, pageable);
        return page.map(LeaveRequestMapper::toLeaveRequestResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> listMyLeaves(Pageable pageable) {
        Long companyId = requireCompanyId();
        Employee emp = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
                .orElseThrow(() -> new BadRequestException("Employee profile not found"));
        return leaveRequestRepository.findByCompanyIdAndEmployeeId(companyId, emp.getId(), pageable)
                .map(LeaveRequestMapper::toLeaveRequestResponse);
    }

    @Override
    @Transactional
    public LeaveRequestResponse review(Long id, ReviewLeaveRequest request) {
        LeaveRequest lr = findInTenant(id);
        if (lr.getStatus() != LeaveRequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING leave requests can be reviewed");
        }
        if (request.getStatus() == LeaveRequestStatus.REJECTED
                && (request.getRejectionReason() == null || request.getRejectionReason().isBlank())) {
            throw new BadRequestException("Rejection reason is required when rejecting a leave requeststatus");
        }

        User reviewer = securityUtil.getCurrentUser();

        lr.setStatus(request.getStatus());
        lr.setRejectionReason(request.getRejectionReason());
        lr.setReviewedBy(reviewer);
        lr.setReviewedAt(LocalDateTime.now());

        // Update leave balance
        leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
                lr.getEmployee().getId(), lr.getLeaveType(), lr.getStartDate().getYear())
                .ifPresent(balance -> {
                    balance.setPendingDays(Math.max(0, balance.getPendingDays() - lr.getTotalDays()));
                    if (request.getStatus() == LeaveRequestStatus.APPROVED) {
                        balance.setUsedDays(balance.getUsedDays() + lr.getTotalDays());
                    }
                });

        if (request.getStatus() == LeaveRequestStatus.APPROVED) {
            // Backdated approval can cover days already marked ABSENT by the
            // nightly absentee job. Flip those specific rows to ON_LEAVE so
            // payroll deductions and attendance reports reflect the approval.
            attendanceRepository.reconcileAbsentToOnLeave(
                    lr.getEmployee().getId(), lr.getStartDate(), lr.getEndDate(),
                    com.businessos.modules.hrm.attendance.attendance.AttendanceStatus.ABSENT,
                    com.businessos.modules.hrm.attendance.attendance.AttendanceStatus.ON_LEAVE);
        }

        if (lr.getEmployee().getUser() != null) {
            try {
                EmailBranding.Data branding = emailBranding.from(lr.getCompany());
                if (request.getStatus() == LeaveRequestStatus.APPROVED) {
                    emailService.sendLeaveApprovalEmail(
                            lr.getEmployee().getUser().getEmail(),
                            lr.getEmployee().getUser().getFirstName(), branding);
                } else if (request.getStatus() == LeaveRequestStatus.REJECTED) {
                    // Previously there was no email method for this at all - a rejected
                    // employee found out only by re-checking the leave list themselves.
                    emailService.sendLeaveRejectionEmail(
                            lr.getEmployee().getUser().getEmail(),
                            lr.getEmployee().getUser().getFirstName(),
                            lr.getRejectionReason(), branding);
                }
            } catch (Exception ex) {
                log.warn("Leave review email failed for employee {}: {}", lr.getEmployee().getUser().getEmail(), ex.getMessage());
            }
        }

        return LeaveRequestMapper.toLeaveRequestResponse(lr);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        LeaveRequest lr = findInTenant(id);
        if (lr.getStatus() == LeaveRequestStatus.APPROVED
                && lr.getStartDate().isBefore(java.time.LocalDate.now())) {
            throw new BadRequestException("Cannot cancel a leave that has already started");
        }
        leaveBalanceRepository.findByEmployeeIdAndLeaveTypeAndYear(
                lr.getEmployee().getId(), lr.getLeaveType(), lr.getStartDate().getYear())
                .ifPresent(balance -> {
                    if (lr.getStatus() == LeaveRequestStatus.PENDING) {
                        balance.setPendingDays(Math.max(0, balance.getPendingDays() - lr.getTotalDays()));
                    } else if (lr.getStatus() == LeaveRequestStatus.APPROVED) {
                        balance.setUsedDays(Math.max(0, balance.getUsedDays() - lr.getTotalDays()));
                    }
                });
        lr.setStatus(LeaveRequestStatus.CANCELLED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getMyBalances(int year) {
        Employee emp = employeeRepository.findByUserId(securityUtil.getCurrentUser().getId())
                .orElseThrow(() -> new BadRequestException("Employee profile not found"));
        return leaveBalanceRepository.findByEmployeeIdAndYear(emp.getId(), year)
                .stream().map(LeaveBalanceMapper::toLeaveBalanceResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> getBalancesForEmployee(Long employeeId, int year) {
        Long companyId = requireCompanyId();
        Employee emp = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return leaveBalanceRepository.findByEmployeeIdAndYear(emp.getId(), year)
                .stream().map(LeaveBalanceMapper::toLeaveBalanceResponse).toList();
    }

    private LeaveRequest findInTenant(Long id) {
        return leaveRequestRepository.findByIdAndCompanyId(id, requireCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Leave requeststatus not found: " + id));
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
}
