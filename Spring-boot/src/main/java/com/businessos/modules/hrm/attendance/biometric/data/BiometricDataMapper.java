package com.businessos.modules.hrm.attendance.biometric.data;

public class BiometricDataMapper {

    public static BiometricDataResponse toResponse(EmployeeBiometricData entity) {
        if (entity == null) {
            return null;
        }

        return BiometricDataResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(entity.getEmployee() != null ? entity.getEmployee().getFullName() : null)
                .deviceId(entity.getDevice() != null ? entity.getDevice().getId() : null)
                .deviceName(entity.getDevice() != null ? entity.getDevice().getDeviceName() : null)
                .biometricType(entity.getBiometricType())
                .biometricTemplate(entity.getBiometricTemplate())
                .templateFormat(entity.getTemplateFormat())
                .enrollmentDate(entity.getEnrollmentDate())
                .enrolledBy(entity.getEnrolledBy())
                .enrollmentAttempts(entity.getEnrollmentAttempts())
                .enrollmentQualityScore(entity.getEnrollmentQualityScore())
                .enrolled(entity.isEnrolled())
                .active(entity.isActive())
                .lastVerifiedTime(entity.getLastVerifiedTime())
                .successfulMatches(entity.getSuccessfulMatches())
                .failedMatches(entity.getFailedMatches())
                .notes(entity.getNotes())
                .securityNotes(entity.getSecurityNotes())
                .build();
    }
}
