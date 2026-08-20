package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

// Returned by POST /api/auth/refresh. Unlike LoginResponse this carries only the token pair —
// role/companyId/userId are unchanged by a refresh, so TokenManager.updateTokens() keeps them.
public class JwtResponse {

    @SerializedName("accessToken")
    private String accessToken;
    @SerializedName("refreshToken")
    private String refreshToken;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
