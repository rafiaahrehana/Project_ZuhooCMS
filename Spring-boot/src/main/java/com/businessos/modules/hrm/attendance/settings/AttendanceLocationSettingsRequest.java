package com.businessos.modules.hrm.attendance.settings;

import lombok.Data;

/**
 * Every field is nullable and only applied when present, so the settings page
 * can send a partial update without wiping the fields it did not render.
 */
@Data
public class AttendanceLocationSettingsRequest {

    private Double officeLatitude;
    private Double officeLongitude;
    private Integer radiusMeters;
    private Boolean gpsEnforcementEnabled;
}
