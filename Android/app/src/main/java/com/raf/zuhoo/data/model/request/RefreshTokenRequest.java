package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class RefreshTokenRequest {

    @SerializedName("refreshToken")
    private final String refreshToken;

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
