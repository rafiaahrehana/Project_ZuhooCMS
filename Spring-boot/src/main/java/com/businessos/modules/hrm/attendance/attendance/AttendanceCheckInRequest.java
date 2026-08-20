package com.businessos.modules.hrm.attendance.attendance;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalTime;

// AllArgsConstructor access is package-private: see ChartOfAccountRequest for why -
// a public one is picked up by Jackson as a deserialization creator, which fails on
// any missing primitive field instead of defaulting it.
@Data @NoArgsConstructor @AllArgsConstructor(access = AccessLevel.PACKAGE) @Builder
public class AttendanceCheckInRequest {

    private Long employeeId;

    private LocalTime checkInTime;

    private AttendanceMethod method;

    private Long deviceId; // If biometric device
    private String latitude; // If GPS
    private String longitude;
    private String location;

    private String reason;
    private boolean verified; // Biometric verification result
    private double verificationScore; // Match score

    /** URL returned by POST /api/upload for a selfie taken at check-in time. */
    private String selfieUrl;
}