package com.businessos.modules.hrm.attendance.biometric.verification;

import com.businessos.modules.hrm.attendance.biometric.data.EmployeeBiometricData;
import com.businessos.modules.hrm.attendance.biometric.data.EmployeeBiometricDataRepository;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BiometricVerificationServiceImpl implements BiometricVerificationService {

    private final EmployeeBiometricDataRepository biometricDataRepository;

    @Override
    @Transactional
    public boolean verifyBiometric(Long employeeId, Long deviceId, String template, double threshold) {
        EmployeeBiometricData biometricData = biometricDataRepository
                .findByEmployeeIdAndDeviceIdAndBiometricType(employeeId, deviceId, "FINGERPRINT")
                .orElseThrow(() -> new ResourceNotFoundException("Biometric data not found"));

        // In production, this would call actual fingerprint matching algorithm
        double matchScore = calculateMatchScore(biometricData.getBiometricTemplate(), template);

        if (matchScore >= threshold) {
            biometricData.recordSuccessfulMatch();
            biometricDataRepository.save(biometricData);
            return true;
        } else {
            biometricData.recordFailedMatch();
            biometricDataRepository.save(biometricData);
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public double getMatchScore(String template1, String template2) {
        // Placeholder: In production, implement actual fingerprint matching algorithm
        // This could use libraries like SourceAFIS or integrate with biometric device APIs
        return calculateMatchScore(template1, template2);
    }

    private double calculateMatchScore(String enrolledTemplate, String capturedTemplate) {
        // Placeholder implementation: Compare template hashes
        // In production, use actual fingerprint matching algorithm
        if (enrolledTemplate == null || capturedTemplate == null) {
            return 0.0;
        }

        // Simple similarity calculation (0-100%)
        // Replace with actual fingerprint matching library
        int matchPoints = 0;
        int totalPoints = Math.min(enrolledTemplate.length(), capturedTemplate.length());

        for (int i = 0; i < totalPoints; i++) {
            if (enrolledTemplate.charAt(i) == capturedTemplate.charAt(i)) {
                matchPoints++;
            }
        }

        return (totalPoints > 0) ? (matchPoints * 100.0 / totalPoints) : 0.0;
    }
}

