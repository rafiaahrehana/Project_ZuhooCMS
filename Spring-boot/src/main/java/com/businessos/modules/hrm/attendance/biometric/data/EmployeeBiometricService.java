package com.businessos.modules.hrm.attendance.biometric.data;

import java.util.List;

public interface EmployeeBiometricService {

    BiometricDataResponse enrollEmployee(BiometricEnrollmentRequest request);
    BiometricDataResponse getEnrollment(Long employeeId, Long deviceId);
    // Direct lookup by the enrollment record's own ID (distinct from getEnrollment,
    // which looks up by employeeId+deviceId pair) - backs the GET /{id} controller endpoint.
    BiometricDataResponse getById(Long id);
    List<BiometricDataResponse> getByEmployee(Long employeeId);

    boolean verifyBiometric(Long employeeId, Long deviceId, String template, double threshold);
    // Overload keyed by the enrollment record's own ID - backs the POST /{id}/verify
    // controller endpoint, which only has the enrollment id, not employeeId+deviceId.
    boolean verifyBiometric(Long id, String template, double threshold);

    void updateEnrollmentStatus(Long id, boolean enrolled);
    void updateLastVerified(Long id);
    void recordSuccessfulMatch(Long id);
    void recordFailedMatch(Long id);

    BiometricDataResponse delete(Long id);
}