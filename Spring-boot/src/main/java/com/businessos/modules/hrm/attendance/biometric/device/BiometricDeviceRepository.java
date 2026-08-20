package com.businessos.modules.hrm.attendance.biometric.device;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BiometricDeviceRepository extends JpaRepository<BiometricDevice, Long> {

    Optional<BiometricDevice> findByCompanyIdAndDeviceId(Long companyId, String deviceId);

    Optional<BiometricDevice> findByIdAndCompanyId(Long id, Long companyId);

    Optional<BiometricDevice> findByCompanyIdAndIpAddress(Long companyId, String ipAddress);

    Page<BiometricDevice> findByCompanyId(Long companyId, Pageable pageable);

    Page<BiometricDevice> findByCompanyIdAndStatus(Long companyId, BiometricDeviceStatus status, Pageable pageable);

    List<BiometricDevice> findByCompanyIdAndIsOnline(Long companyId, boolean online);
}
