package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class AttendanceLocationSettingsResponse {

    @SerializedName("officeLatitude")
    private Double officeLatitude;
    @SerializedName("officeLongitude")
    private Double officeLongitude;
    @SerializedName("radiusMeters")
    private Integer radiusMeters;
    @SerializedName("gpsEnforcementEnabled")
    private boolean gpsEnforcementEnabled;

    public Double getOfficeLatitude() {
        return officeLatitude;
    }

    public Double getOfficeLongitude() {
        return officeLongitude;
    }

    public Integer getRadiusMeters() {
        return radiusMeters;
    }

    public boolean isGpsEnforcementEnabled() {
        return gpsEnforcementEnabled;
    }
}
