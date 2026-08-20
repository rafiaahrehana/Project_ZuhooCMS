package com.businessos.modules.hrm.attendance.settings;

import com.businessos.security.SecurityUtil;
import com.businessos.shared.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Reads and applies one company's office-location policy for attendance.
 *
 * AttendanceServiceImpl calls {@link #getOrCreate} on every check-in/check-out
 * rather than querying the repository itself, so there is a single place that
 * decides "does this company enforce GPS, and against what".
 */
@Service
@RequiredArgsConstructor
public class AttendanceLocationSettingsService {

    private final AttendanceLocationSettingsRepository repository;
    private final SecurityUtil securityUtil;

    /**
     * The company's settings, creating the default row on first access so
     * callers never deal with an empty Optional.
     *
     * Same read-only-transaction guard as PayrollSettingsService.getOrCreate:
     * a read path (e.g. a check-in inside a transaction that turns out to be
     * read-only elsewhere) must not attempt a save. When the row is missing,
     * a defaults instance (enforcement off) is returned unsaved instead; the
     * row is actually written the first time the settings PUT is used.
     */
    @Transactional
    public AttendanceLocationSettings getOrCreate(Long companyId) {
        return repository.findByCompanyId(companyId)
                .orElseGet(() -> {
                    AttendanceLocationSettings defaults =
                            AttendanceLocationSettings.builder().companyId(companyId).build();
                    if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                        return defaults;
                    }
                    return repository.save(defaults);
                });
    }

    @Transactional
    public AttendanceLocationSettings getOrCreateForCurrentCompany() {
        Long companyId = securityUtil.getCurrentCompanyId();
        if (companyId == null) {
            throw new BadRequestException("No company context for the current user");
        }
        return getOrCreate(companyId);
    }

    @Transactional
    public AttendanceLocationSettings update(AttendanceLocationSettingsRequest request) {
        AttendanceLocationSettings settings = getOrCreateForCurrentCompany();

        if (request.getOfficeLatitude() != null) {
            settings.setOfficeLatitude(requireRange(request.getOfficeLatitude(), -90.0, 90.0, "Office latitude"));
        }
        if (request.getOfficeLongitude() != null) {
            settings.setOfficeLongitude(requireRange(request.getOfficeLongitude(), -180.0, 180.0, "Office longitude"));
        }
        if (request.getRadiusMeters() != null) {
            if (request.getRadiusMeters() < 10 || request.getRadiusMeters() > 5000) {
                throw new BadRequestException("Radius must be between 10 and 5000 meters.");
            }
            settings.setRadiusMeters(request.getRadiusMeters());
        }
        if (request.getGpsEnforcementEnabled() != null) {
            settings.setGpsEnforcementEnabled(request.getGpsEnforcementEnabled());
        }

        return repository.save(settings);
    }

    private double requireRange(double value, double min, double max, String label) {
        if (value < min || value > max) {
            throw new BadRequestException(label + " must be between " + min + " and " + max + ".");
        }
        return value;
    }
}
