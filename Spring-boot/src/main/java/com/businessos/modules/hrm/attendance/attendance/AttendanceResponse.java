package com.businessos.modules.hrm.attendance.attendance;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {

    private Long id;
    private Long companyId;

    // Employee info
    private Long employeeId;
    private String employeeName;
    private String employeeNumber;

    // Date
    private LocalDate attendanceDate;

    // Check-in
    private LocalTime checkInTime;
    private LocalDateTime checkInDateTime;
    private AttendanceMethod checkInMethod;
    private String checkInLocation;
    private String checkInLatitude;
    private String checkInLongitude;
    private String checkInReason;
    private String checkInSelfieUrl;

    // Check-out
    private LocalTime checkOutTime;
    private LocalDateTime checkOutDateTime;
    private AttendanceMethod checkOutMethod;
    private String checkOutLocation;
    private String checkOutSelfieUrl;

    // Location verification
    private boolean locationFlagged;
    private String locationFlagReason;
    private Double distanceFromOfficeMeters;

    // Status
    private AttendanceStatus status;
    private ShiftType shiftType;

    // Late tracking
    /**
     * Serialised as "isLate", not Jackson's default "late".
     *
     * For a boolean named isLate, Jackson derives the property from the getter
     * isLate() and emits "late". The frontend has always read isLate, so the
     * Late column silently rendered empty for every record. Pinning the name
     * here fixes it without a rename rippling through the client.
     */
    @com.fasterxml.jackson.annotation.JsonProperty("isLate")
    private boolean isLate;
    private long lateMinutes;
    private String lateReason;

    // Overtime tracking
    /** Pinned like isLate above: Jackson would otherwise emit "overtime", which the frontend never reads. */
    @com.fasterxml.jackson.annotation.JsonProperty("isOvertime")
    private boolean isOvertime;
    private BigDecimal overtimeHours;

    // Early departure
    private boolean leftEarly;
    private long earlyMinutes;
    private String earlyDepartureReason;

    // Hours
    private BigDecimal totalWorkingHours;

    // Biometric
    /** Pinned like isLate above: Jackson would otherwise emit "verified", which the frontend never reads. */
    @com.fasterxml.jackson.annotation.JsonProperty("isVerified")
    private boolean isVerified;
    private double verificationScore;

    // Approval
    private boolean approved;
    private String approvedBy;
    private LocalDateTime approvedDateTime;

    // Notes
    private String notes;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
