package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class RegisterDeviceTokenRequest {

    private static final String PLATFORM_ANDROID = "ANDROID";

    @SerializedName("token")
    private final String token;
    @SerializedName("platform")
    private final String platform;

    public RegisterDeviceTokenRequest(String token) {
        this.token = token;
        this.platform = PLATFORM_ANDROID;
    }
}
