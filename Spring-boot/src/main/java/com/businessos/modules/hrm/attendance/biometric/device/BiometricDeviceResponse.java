package com.businessos.modules.hrm.attendance.biometric.device;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricDeviceResponse {
    
    private Long id;
    private Long companyId;
    
    private String deviceName;
    private BiometricDeviceType deviceType;
    private String deviceId;
    private String ipAddress;
    private int portNumber;
    
    private String location;
    private String department;
    
    private BiometricDeviceStatus status;
    private int matchThreshold;
    
    private boolean enabledForCheckIn;
    private boolean enabledForCheckOut;
    
    private LocalDateTime lastSyncTime;
    private LocalDateTime lastHealthCheckTime;
    private boolean isOnline;
    
    private String manufacturer;
    private String model;
    private String firmwareVersion;
    
    private int totalEnrollments;
    private int maxEnrollments;
    
    private String notes;
}
