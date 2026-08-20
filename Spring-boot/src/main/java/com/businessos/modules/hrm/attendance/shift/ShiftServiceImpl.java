package com.businessos.modules.hrm.attendance.shift;

import com.businessos.modules.company.Company;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.security.SecurityUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor

public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    private final com.businessos.modules.hrm.employee.EmployeeRepository employeeRepository;
    private final SecurityUtil    securityUtil;
    private final AuthorizationService authorizationService;

    @Override
    @Transactional
    public ShiftResponse create(ShiftRequest request) {
        authorizationService.checkPermission(PermissionCode.SHIFT_CREATE);
        Long companyId = requireCompanyId();
        if (shiftRepository.existsByCompanyIdAndName(companyId, request.getName())) {
            throw new BadRequestException("Shift '" + request.getName() + "' already exists");
        }
        Shift s = Shift.builder()
            .name(request.getName())
            .shiftType(request.getShiftType())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .gracePeriodMinutes(request.getGracePeriodMinutes() != null ? request.getGracePeriodMinutes() : 10)
            // Matches Shift.weeklyOffDays' own default - was "SAT,SUN" here, a
            // mismatch that was functionally dormant (both are just fallbacks
            // for a field almost every request already supplies) but confusing.
            .weeklyOffDays(request.getWeeklyOffDays() != null ? request.getWeeklyOffDays() : "FRI,SAT")
            .flexible(request.isFlexible())
            .nightShift(request.isNightShift())
            .workingMinutes(request.getStartTime() != null && request.getEndTime() != null ? java.time.temporal.ChronoUnit.MINUTES.between(request.getStartTime(), request.getEndTime()) : 0)
            .description(request.getDescription())
            .notes(request.getNotes())
            .company(companyRef(companyId))
            .build();
        shiftRepository.save(s);
        return ShiftMapper.toShiftResponse(s);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftResponse getById(Long id) {
        return ShiftMapper.toShiftResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftResponse> listAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SHIFT_VIEW);
        return shiftRepository.findByCompanyId(requireCompanyId(), pageable)
            .map(ShiftMapper::toShiftResponse);
    }

    // Deliberately NOT gated by SHIFT_VIEW: this is the active-shift picker consumed by
    // the Employees form and Attendance Shift Assignments page - users with
    // EMPLOYEE_UPDATE/SHIFT_ASSIGNMENT_VIEW but not SHIFT_VIEW still need it.
    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponse> listActive() {
        return shiftRepository.findByCompanyIdAndActiveTrue(requireCompanyId())
            .stream().map(ShiftMapper::toShiftResponse).toList();
    }

    @Override
    @Transactional
    public ShiftResponse update(Long id, ShiftRequest request) {
        authorizationService.checkPermission(PermissionCode.SHIFT_UPDATE);
        Shift s = findInTenant(id);
        s.setName(request.getName());
        s.setShiftType(request.getShiftType());
        s.setStartTime(request.getStartTime());
        s.setEndTime(request.getEndTime());
        if (request.getGracePeriodMinutes() != null) s.setGracePeriodMinutes(request.getGracePeriodMinutes());
        if (request.getWeeklyOffDays() != null) s.setWeeklyOffDays(request.getWeeklyOffDays());
        s.setFlexible(request.isFlexible());
        s.setNightShift(request.isNightShift());
        s.setWorkingMinutes(request.getStartTime() != null && request.getEndTime() != null ? java.time.temporal.ChronoUnit.MINUTES.between(request.getStartTime(), request.getEndTime()) : 0);
        s.setDescription(request.getDescription());
        s.setNotes(request.getNotes());
        return ShiftMapper.toShiftResponse(s);
    }

    @Override
    @Transactional
    public ShiftResponse toggleActive(Long id) {
        authorizationService.checkPermission(PermissionCode.SHIFT_UPDATE);
        Shift s = findInTenant(id);
        s.setActive(!s.isActive());
        return ShiftMapper.toShiftResponse(s);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.SHIFT_DELETE);
        Shift shift = findInTenant(id);
        long assignedEmployees = employeeRepository.countByShiftId(id);
        if (assignedEmployees > 0) {
            throw new BadRequestException(
                "Cannot delete this shift: it is currently assigned to " + assignedEmployees
                    + " employee(s). Reassign them first.");
        }
        shift.softDelete();
    }

    private Shift findInTenant(Long id) {
        return shiftRepository.findByIdAndCompanyId(id, requireCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + id));
    }

    private Long requireCompanyId() {
        Long id = securityUtil.getCurrentCompanyId();
        if (id == null) throw new BadRequestException("No company context");
        return id;
    }

    private Company companyRef(Long companyId) {
        Company c = new Company(); c.setId(companyId); return c;
    }
}
