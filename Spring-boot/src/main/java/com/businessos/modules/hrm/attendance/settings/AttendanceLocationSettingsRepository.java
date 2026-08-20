package com.businessos.modules.hrm.attendance.settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceLocationSettingsRepository extends JpaRepository<AttendanceLocationSettings, Long> {

    Optional<AttendanceLocationSettings> findByCompanyId(Long companyId);
}
