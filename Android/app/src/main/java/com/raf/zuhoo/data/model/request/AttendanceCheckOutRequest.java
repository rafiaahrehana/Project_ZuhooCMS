package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class AttendanceCheckOutRequest {

    @SerializedName("method")
    private final String method = "GPS";
    @SerializedName("latitude")
    private final String latitude;
    @SerializedName("longitude")
    private final String longitude;
    // Optional server-side, but the check-in screen always captures one before enabling the
    // check-out button, same as check-in.
    @SerializedName("selfieUrl")
    private final String selfieUrl;

    public AttendanceCheckOutRequest(String latitude, String longitude, String selfieUrl) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.selfieUrl = selfieUrl;
    }
}
