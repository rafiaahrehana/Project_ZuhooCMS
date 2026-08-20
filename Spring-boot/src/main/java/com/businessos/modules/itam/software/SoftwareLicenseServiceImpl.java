package com.businessos.modules.itam.software;

import com.businessos.modules.hrm.employee.Employee;
import com.businessos.modules.hrm.employee.EmployeeRepository;
import com.businessos.auth.role.enums.PermissionCode;
import com.businessos.auth.role.service.AuthorizationService;
import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.ResourceNotFoundException;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SoftwareLicenseServiceImpl implements SoftwareLicenseService {

    private final SoftwareLicenseRepository licenseRepository;
    private final SoftwareLicenseSeatRepository seatRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityUtil securityUtil;
    private final AuthorizationService authorizationService;

    private SoftwareLicense findInTenant(Long id) {
        return licenseRepository.findByIdAndCompanyId(id, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("License not found"));
    }

    @Override
    @Transactional
    public SoftwareLicenseResponse create(SoftwareLicenseRequest request) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_CREATE);
        Long companyId = securityUtil.getCurrentCompanyId();

        if (licenseRepository.findByCompanyIdAndLicenseKey(companyId, request.getLicenseKey()).isPresent()) {
            throw new BadRequestException("License key already exists: " + request.getLicenseKey());
        }

        SoftwareLicense license = SoftwareLicenseMapper.toEntity(request);
        license.setCompanyId(companyId);
        license.setLicenseStatus(LicenseStatus.ACTIVE);
        license.setSeatsAvailable(license.getTotalSeatsLicensed());

        license = licenseRepository.save(license);
        return SoftwareLicenseMapper.toResponse(license);
    }

    @Override
    @Transactional(readOnly = true)
    public SoftwareLicenseResponse getById(Long id) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_VIEW);
        return SoftwareLicenseMapper.toResponse(findInTenant(id));
    }

    @Override
    @Transactional(readOnly = true)
    public SoftwareLicenseResponse getByLicenseKey(String key) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        SoftwareLicense license = licenseRepository.findByCompanyIdAndLicenseKey(companyId, key)
                .orElseThrow(() -> new ResourceNotFoundException("License not found"));
        return SoftwareLicenseMapper.toResponse(license);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SoftwareLicenseResponse> getAll(Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return licenseRepository.findByCompanyId(companyId, pageable)
                .map(SoftwareLicenseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SoftwareLicenseResponse> getByStatus(LicenseStatus status, Pageable pageable) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return licenseRepository.findByCompanyIdAndLicenseStatus(companyId, status, pageable)
                .map(SoftwareLicenseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoftwareLicenseResponse> getExpiringLicenses() {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        LocalDate thirtyDaysFromNow = LocalDate.now().plusDays(30);
        return licenseRepository.findExpiringBetweenDates(companyId, LocalDate.now(), thirtyDaysFromNow)
                .stream()
                .map(SoftwareLicenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoftwareLicenseResponse> getExpiredLicenses() {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return licenseRepository.findExpiredLicenses(companyId, LocalDate.now())
                .stream()
                .map(SoftwareLicenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SoftwareLicenseResponse update(Long id, SoftwareLicenseRequest request) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_UPDATE);
        SoftwareLicense license = findInTenant(id);

        if (request.getTotalSeatsLicensed() < license.getSeatsUsed()) {
            throw new BadRequestException(
                    "Cannot reduce total seats below the " + license.getSeatsUsed() + " currently assigned");
        }

        license.setSoftwareName(request.getSoftwareName());
        license.setPublisher(request.getPublisher());
        license.setVersion(request.getVersion());
        license.setLicenseType(request.getLicenseType());
        license.setTotalSeatsLicensed(request.getTotalSeatsLicensed());
        license.setSeatsAvailable(request.getTotalSeatsLicensed() - license.getSeatsUsed());
        license.setLicensePurchaseDate(request.getLicensePurchaseDate());
        license.setLicenseCost(request.getLicenseCost());
        license.setLicenseExpiryDate(request.getLicenseExpiryDate());
        license.setRenewalType(request.getRenewalType());
        license.setNextRenewalDate(request.getNextRenewalDate());
        license.setRenewalCost(request.getRenewalCost());
        license.setVendor(request.getVendor());
        license.setAccountEmail(request.getAccountEmail());
        license.setLicenseUrl(request.getLicenseUrl());
        license.setInstallationLocation(request.getInstallationLocation());
        license.setEstimatedUserCount(request.getEstimatedUserCount());
        license.setComplianceNotes(request.getComplianceNotes());
        license.setAutoRenew(request.isAutoRenewOrDefault());
        license.setNotes(request.getNotes());
        license.setRenewalNotes(request.getRenewalNotes());

        license = licenseRepository.save(license);
        return SoftwareLicenseMapper.toResponse(license);
    }

    @Override
    @Transactional
    public void assignSeat(Long licenseId, Long employeeId) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_ASSIGN);
        Long companyId = securityUtil.getCurrentCompanyId();
        SoftwareLicense license = licenseRepository.findByIdAndCompanyIdForUpdate(licenseId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("License not found"));
        Employee employee = employeeRepository.findByIdAndCompanyId(employeeId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        if (license.getSeatsAvailable() <= 0) {
            throw new BadRequestException("No seats available");
        }
        if (seatRepository.findByLicenseIdAndEmployeeIdAndReleasedAtIsNull(licenseId, employeeId).isPresent()) {
            throw new BadRequestException(employee.getUser().getFullName() + " already has a seat on this license");
        }

        license.assignSeat();
        licenseRepository.save(license);

        seatRepository.save(SoftwareLicenseSeat.builder()
                .companyId(companyId)
                .license(license)
                .employee(employee)
                .assignedAt(LocalDate.now())
                .build());
    }

    @Override
    @Transactional
    public void releaseSeat(Long licenseId, Long employeeId) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_ASSIGN);
        SoftwareLicense license = licenseRepository
                .findByIdAndCompanyIdForUpdate(licenseId, securityUtil.getCurrentCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("License not found"));

        SoftwareLicenseSeat seat = seatRepository
                .findByLicenseIdAndEmployeeIdAndReleasedAtIsNull(licenseId, employeeId)
                .orElseThrow(() -> new BadRequestException("This employee does not currently hold a seat on this license"));

        seat.setReleasedAt(LocalDate.now());
        seatRepository.save(seat);

        license.releaseSeat();
        licenseRepository.save(license);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoftwareLicenseSeatResponse> getSeatHolders(Long licenseId) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_VIEW);
        findInTenant(licenseId); // ownership check
        return seatRepository.findByLicenseIdAndReleasedAtIsNull(licenseId)
                .stream()
                .map(this::toSeatResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SoftwareLicenseSeatResponse> getLicensesForEmployee(Long employeeId) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_VIEW);
        Long companyId = securityUtil.getCurrentCompanyId();
        return seatRepository.findByEmployeeIdAndCompanyIdAndReleasedAtIsNull(employeeId, companyId)
                .stream()
                .map(this::toSeatResponse)
                .collect(Collectors.toList());
    }

    private SoftwareLicenseSeatResponse toSeatResponse(SoftwareLicenseSeat seat) {
        return SoftwareLicenseSeatResponse.builder()
                .id(seat.getId())
                .licenseId(seat.getLicense().getId())
                .softwareName(seat.getLicense().getSoftwareName())
                .employeeId(seat.getEmployee().getId())
                .employeeName(seat.getEmployee().getUser().getFullName())
                .assignedAt(seat.getAssignedAt())
                .build();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        authorizationService.checkPermission(PermissionCode.SOFTWARE_LICENSE_DELETE);
        SoftwareLicense license = findInTenant(id);
        long activeSeats = seatRepository.findByLicenseIdAndReleasedAtIsNull(id).size();
        if (activeSeats > 0) {
            throw new BadRequestException(
                    "Cannot delete license with " + activeSeats + " active seat(s). Release all seats first.");
        }
        license.softDelete();
        licenseRepository.save(license);
    }
}
