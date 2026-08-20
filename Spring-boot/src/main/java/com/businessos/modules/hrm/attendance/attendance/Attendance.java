package com.businessos.modules.hrm.attendance.attendance;

import com.businessos.core.base.BaseEntity;
import com.businessos.modules.hrm.attendance.biometric.device.BiometricDevice;
import com.businessos.modules.hrm.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "companyId", type = Long.class))
@Filter(name = "tenantFilter", condition = "company_id = :companyId")
@Entity
@Table(name = "attendance", indexes = {
        @Index(name = "idx_attendance_employee", columnList = "employee_id"),
        @Index(name = "idx_attendance_date", columnList = "attendance_date"),
        @Index(name = "idx_attendance_status", columnList = "status"),
        @Index(name = "idx_attendance_company", columnList = "company_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance extends BaseEntity {

    private Long companyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private LocalDate attendanceDate;

    // Check-in details
    private LocalTime checkInTime;
    private LocalDateTime checkInDateTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AttendanceMethod checkInMethod = AttendanceMethod.MANUAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_device_id")
    private BiometricDevice checkInDevice;

    private String checkInLatitude;
    private String checkInLongitude;
    private String checkInLocation;

    private String checkInReason; // Optional reason for entry

    private String checkInSelfieUrl;

    @Builder.Default
    private boolean isVerified = false; // Biometric verification status

    @Builder.Default
    private double verificationScore = 0.0; // 0-100% match score for fingerprint

    // Location verification (GPS geofence against AttendanceLocationSettings)
    // columnDefinition carries an explicit DEFAULT so Hibernate's generated ALTER TABLE succeeds
    // on the existing (non-empty) attendance table — a NOT NULL column added with no default
    // fails on Postgres once rows already exist.
    @Builder.Default
    @Column(columnDefinition = "boolean default false", nullable = false)
    private boolean locationFlagged = false;

    private String locationFlagReason;

    private Double distanceFromOfficeMeters;

    // Check-out details
    private LocalTime checkOutTime;
    private LocalDateTime checkOutDateTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AttendanceMethod checkOutMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_device_id")
    private BiometricDevice checkOutDevice;

    private String checkOutLatitude;
    private String checkOutLongitude;
    private String checkOutLocation;

    private String checkOutSelfieUrl;

    // Status
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ShiftType shiftType;

    // Late tracking
    @Builder.Default
    private boolean isLate = false;

    private long lateMinutes; // How many minutes late

    private String lateReason;

    // Overtime tracking
    @Builder.Default
    private boolean isOvertime = false;

    private BigDecimal overtimeHours;

    // Early departure
    @Builder.Default
    private boolean leftEarly = false;

    private long earlyMinutes;

    private String earlyDepartureReason;

    // Total hours
    private BigDecimal totalWorkingHours;

    // Notes
    private String adminNotes;
    private String approvalNotes;

    // Previously defaulted to true, so every self-service check-in started
    // "approved" with no one having approved anything - approveAttendance()
    // existed but had nothing left to actually do. Payroll reads status, not
    // this field, so the default change doesn't affect pay calculation.
    @Builder.Default
    private boolean approved = false;

    private String approvedBy;
    private LocalDateTime approvedDateTime;

    public long calculateTotalMinutes() {
        if (checkInTime != null && checkOutTime != null) {
            return java.time.temporal.ChronoUnit.MINUTES.between(checkInTime, checkOutTime);
        }
        return 0;
    }

    public BigDecimal calculateTotalHours() {
        long minutes = calculateTotalMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
    }

    public void checkIn(LocalTime time, AttendanceMethod method, BiometricDevice device) {
        this.checkInTime = time;
        this.checkInDateTime = LocalDateTime.now();
        this.checkInMethod = method;
        this.checkInDevice = device;
    }

    public void checkOut(LocalTime time, AttendanceMethod method, BiometricDevice device) {
        this.checkOutTime = time;
        this.checkOutDateTime = LocalDateTime.now();
        this.checkOutMethod = method;
        this.checkOutDevice = device;
        this.totalWorkingHours = calculateTotalHours();
    }

    public boolean isCompleteDay() {
        return checkInTime != null && checkOutTime != null;
    }
}
