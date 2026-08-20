package com.businessos.modules.hrm.attendance.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceCheckOutRequest {

    private LocalTime checkOutTime;

    private AttendanceMethod method;

    private Long deviceId;       // If biometric device used for check-out

    private String latitude;     // If GPS-based check-out
    private String longitude;
    private String location;

    private String earlyDepartureReason;  // Optional reason if leaving before shift end

    /** URL returned by POST /api/upload for a selfie taken at check-out time. Optional. */
    private String selfieUrl;
}
