package com.businessos.modules.hrm.attendance.biometric.device;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// AllArgsConstructor access is package-private: a public one is picked up by Jackson as
// a deserialization creator, which fails on any missing primitive field ("Cannot map
// null into type int/boolean") instead of defaulting it to 0/false via no-args+setters.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class BiometricDeviceRequest {

    @NotBlank(message = "Device name is required")
    private String deviceName;

    @NotNull(message = "Device type is required")
    private BiometricDeviceType deviceType;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    private String ipAddress;
    private int portNumber;

    private String location;
    private String department;

    private Integer matchThreshold;
    private Boolean enabledForCheckIn;
    private Boolean enabledForCheckOut;

    private String notes;
}
