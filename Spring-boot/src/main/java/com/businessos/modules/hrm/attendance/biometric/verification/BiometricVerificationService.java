package com.businessos.modules.hrm.attendance.biometric.verification;

public interface BiometricVerificationService {
    boolean verifyBiometric(Long employeeId, Long deviceId, String template, double threshold);
    double getMatchScore(String template1, String template2);
}
