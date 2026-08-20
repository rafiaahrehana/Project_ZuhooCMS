package com.businessos.modules.hrm.leave.holiday;

public class HolidayMapper {
    public static HolidayResponse toHolidayResponse(Holiday h) {
        HolidayResponse r = new HolidayResponse();
        r.setId(h.getId());
        r.setName(h.getName());
        r.setHolidayDate(h.getDate());
        r.setHolidayType(h.getType());
        r.setDescription(h.getDescription());
        r.setCreatedAt(h.getCreatedAt());
        return r;
    }
}
