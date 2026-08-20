package com.businessos.modules.hrm.attendance.shift;

public class ShiftMapper {

    public static ShiftResponse toShiftResponse(Shift s) {
        ShiftResponse r = new ShiftResponse();
        r.setId(s.getId());
        r.setName(s.getName());
        r.setShiftType(s.getShiftType());
        r.setStartTime(s.getStartTime());
        r.setEndTime(s.getEndTime());
        r.setGracePeriodMinutes(s.getGracePeriodMinutes());
        r.setWeeklyOffDays(s.getWeeklyOffDays());
        r.setFlexible(s.isFlexible());
        r.setNightShift(s.isNightShift());
        r.setActive(s.isActive());
        r.setWorkingMinutes(s.getWorkingMinutes());
        r.setDescription(s.getDescription());
        r.setNotes(s.getNotes());
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }
}
