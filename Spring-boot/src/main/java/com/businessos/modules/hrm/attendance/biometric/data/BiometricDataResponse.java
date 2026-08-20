package com.businessos.modules.hrm.attendance.biometric.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricDataResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long deviceId;
    private String deviceName;
    private String biometricType;
    private String biometricTemplate;
    private String templateFormat;
    private LocalDateTime enrollmentDate;
    private String enrolledBy;
    private int enrollmentAttempts;
    private double enrollmentQualityScore;
    private boolean enrolled;
    private boolean active;
    private LocalDateTime lastVerifiedTime;
    private int successfulMatches;
    private int failedMatches;
    private String notes;
    private String securityNotes;
}
