package com.businessos.modules.hrm.attendance.biometric.data;

import jakarta.validation.constraints.*;
import lombok.*;

// AllArgsConstructor access is package-private: see ChartOfAccountRequest for why -
// a public one is picked up by Jackson as a deserialization creator, which fails on
// any missing primitive field instead of defaulting it.
@Data @NoArgsConstructor @AllArgsConstructor(access = AccessLevel.PACKAGE) @Builder
public class BiometricEnrollmentRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Device ID is required")
    private Long deviceId;

    @NotBlank(message = "Biometric type is required")
    private String biometricType; // FINGERPRINT, FACIAL, etc.

    @NotBlank(message = "Biometric template is required")
    private String biometricTemplate; // Base64 encoded template

    private String templateFormat;

    @Min(value = 0)
    @Max(value = 100)
    private double qualityScore;
}
