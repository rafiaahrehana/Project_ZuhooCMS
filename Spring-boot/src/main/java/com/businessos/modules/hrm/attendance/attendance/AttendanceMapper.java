package com.businessos.modules.hrm.attendance.attendance;

public class AttendanceMapper {

    public static AttendanceResponse toResponse(Attendance entity) {
        if (entity == null) return null;

        return AttendanceResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                // Employee
                .employeeId(entity.getEmployee() != null ? entity.getEmployee().getId() : null)
                .employeeName(entity.getEmployee() != null ? entity.getEmployee().getFullName() : null)
                .employeeNumber(entity.getEmployee() != null ? entity.getEmployee().getEmployeeNumber() : null)
                // Date
                .attendanceDate(entity.getAttendanceDate())
                // Check-in
                .checkInTime(entity.getCheckInTime())
                .checkInDateTime(entity.getCheckInDateTime())
                .checkInMethod(entity.getCheckInMethod())
                .checkInLocation(entity.getCheckInLocation())
                .checkInLatitude(entity.getCheckInLatitude())
                .checkInLongitude(entity.getCheckInLongitude())
                .checkInReason(entity.getCheckInReason())
                .checkInSelfieUrl(entity.getCheckInSelfieUrl())
                // Check-out
                .checkOutTime(entity.getCheckOutTime())
                .checkOutDateTime(entity.getCheckOutDateTime())
                .checkOutMethod(entity.getCheckOutMethod())
                .checkOutLocation(entity.getCheckOutLocation())
                .checkOutSelfieUrl(entity.getCheckOutSelfieUrl())
                // Location verification
                .locationFlagged(entity.isLocationFlagged())
                .locationFlagReason(entity.getLocationFlagReason())
                .distanceFromOfficeMeters(entity.getDistanceFromOfficeMeters())
                // Status
                .status(entity.getStatus())
                .shiftType(entity.getShiftType())
                // Late
                .isLate(entity.isLate())
                .lateMinutes(entity.getLateMinutes())
                .lateReason(entity.getLateReason())
                // Overtime
                .isOvertime(entity.isOvertime())
                .overtimeHours(entity.getOvertimeHours())
                // Early departure
                .leftEarly(entity.isLeftEarly())
                .earlyMinutes(entity.getEarlyMinutes())
                .earlyDepartureReason(entity.getEarlyDepartureReason())
                // Hours
                .totalWorkingHours(entity.getTotalWorkingHours())
                // Biometric
                .isVerified(entity.isVerified())
                .verificationScore(entity.getVerificationScore())
                // Approval
                .approved(entity.isApproved())
                .approvedBy(entity.getApprovedBy())
                .approvedDateTime(entity.getApprovedDateTime())
                // Notes
                .notes(entity.getAdminNotes())
                // Audit
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}