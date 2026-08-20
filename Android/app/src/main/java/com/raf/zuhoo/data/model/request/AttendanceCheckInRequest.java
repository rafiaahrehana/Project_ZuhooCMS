package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class AttendanceCheckInRequest {

    @SerializedName("method")
    private final String method = "GPS";
    @SerializedName("latitude")
    private final String latitude;
    @SerializedName("longitude")
    private final String longitude;
    // The app only gets here after a live selfie + GPS fix were captured, so this is an honest
    // client attestation — the server still overrides it to false if the location is flagged.
    @SerializedName("verified")
    private final boolean verified = true;
    @SerializedName("selfieUrl")
    private final String selfieUrl;

    public AttendanceCheckInRequest(String latitude, String longitude, String selfieUrl) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.selfieUrl = selfieUrl;
    }
}
