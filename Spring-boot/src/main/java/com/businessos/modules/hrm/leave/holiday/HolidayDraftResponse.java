package com.businessos.modules.hrm.leave.holiday;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDraftResponse {
    private String name;
    private String date;
    private String type;
    private String description;
}
