package com.businessos.modules.hrm.attendance.biometric.device;

import com.businessos.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.time.LocalDateTime;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "biometric_devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BiometricDevice extends BaseEntity {

    private Long companyId; // Tenant isolation

    private String deviceName; // "Main Gate Fingerprint Terminal"

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BiometricDeviceType deviceType;

    private String deviceId; // Serial number or unique ID
    private String ipAddress; // For network-based devices
    private int portNumber;

    private String location; // "Main Office - Ground Floor"
    private String department; // "Administration"

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BiometricDeviceStatus status = BiometricDeviceStatus.ACTIVE;

    // API Integration
    private String apiEndpoint;
    private String apiKey;
    private String apiSecret;

    // Configuration
    @Builder.Default
    private int matchThreshold = 95; // 95% for fingerprint

    @Builder.Default
    private boolean enabledForCheckIn = true;

    @Builder.Default
    private boolean enabledForCheckOut = true;

    // Sync & Communication
    private LocalDateTime lastSyncTime;
    private LocalDateTime lastHealthCheckTime;

    @Builder.Default
    private boolean isOnline = true;

    private String manufacturer; // "Suprema", "ZKTeco", etc.
    private String model;
    private String firmwareVersion;

    // Capacity
    @Builder.Default
    private int totalEnrollments = 0;

    @Builder.Default
    private int maxEnrollments = 5000;

    // Notes
    private String notes;
    private String maintenanceNotes;

    public boolean isAtCapacity() {
        return totalEnrollments >= maxEnrollments;
    }

    public boolean canAddEnrollments(int count) {
        return (totalEnrollments + count) <= maxEnrollments;
    }
}
