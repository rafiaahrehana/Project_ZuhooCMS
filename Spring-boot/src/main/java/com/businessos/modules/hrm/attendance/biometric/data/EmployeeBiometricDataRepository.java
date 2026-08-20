package com.businessos.modules.hrm.attendance.biometric.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeBiometricDataRepository extends JpaRepository<EmployeeBiometricData, Long> {

    Optional<EmployeeBiometricData> findByEmployeeIdAndDeviceIdAndBiometricType(Long employeeId, Long deviceId, String type);

    List<EmployeeBiometricData> findByEmployeeId(Long employeeId);

    List<EmployeeBiometricData> findByDeviceIdAndEnrolled(Long deviceId, boolean enrolled);

    long countByDeviceId(Long deviceId);
}