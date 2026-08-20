package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class UpdateAttendanceLocationSettingsRequest {

    @SerializedName("officeLatitude")
    private final double officeLatitude;
    @SerializedName("officeLongitude")
    private final double officeLongitude;
    @SerializedName("radiusMeters")
    private final int radiusMeters;
    @SerializedName("gpsEnforcementEnabled")
    private final boolean gpsEnforcementEnabled;

    public UpdateAttendanceLocationSettingsRequest(double officeLatitude, double officeLongitude,
                                                    int radiusMeters, boolean gpsEnforcementEnabled) {
        this.officeLatitude = officeLatitude;
        this.officeLongitude = officeLongitude;
        this.radiusMeters = radiusMeters;
        this.gpsEnforcementEnabled = gpsEnforcementEnabled;
    }
}
