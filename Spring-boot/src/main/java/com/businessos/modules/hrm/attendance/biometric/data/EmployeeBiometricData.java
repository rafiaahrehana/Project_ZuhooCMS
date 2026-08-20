package com.businessos.modules.hrm.attendance.biometric.data;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.attendance.biometric.device.BiometricDevice;
import com.businessos.modules.hrm.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_biometric_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"employee_id", "device_id", "biometric_type"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeBiometricData extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private BiometricDevice device;

    private String biometricType; // FINGERPRINT, FACIAL, IRIS, RFID_CARD_ID

    private String biometricTemplate; // Hash/template of biometric data (never plain data)
    private String templateFormat; // ISO19794, WSQ, JPEG for different formats

    private LocalDateTime enrollmentDate;
    private String enrolledBy; // Admin/HR who enrolled

    @Builder.Default
    private int enrollmentAttempts = 0; // How many attempts to get good template

    @Builder.Default
    private double enrollmentQualityScore = 0.0; // 0-100

    @Builder.Default
    private boolean enrolled = true; // Enrollment status

    @Builder.Default
    private boolean active = true; // Can be deactivated

    private LocalDateTime lastVerifiedTime;

    @Builder.Default
    private int successfulMatches = 0; // Successful verification count

    @Builder.Default
    private int failedMatches = 0; // Failed verification attempts

    private String notes;
    private String securityNotes; // Enrollment security remarks

    public void recordSuccessfulMatch() {
        this.successfulMatches++;
        this.lastVerifiedTime = LocalDateTime.now();
    }

    public void recordFailedMatch() {
        this.failedMatches++;
    }
}
