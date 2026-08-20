package com.raf.zuhoo.data.model.response;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {

    @SerializedName("userId")
    private Long userId;
    @SerializedName("firstName")
    private String firstName;
    @SerializedName("email")
    private String email;
    @SerializedName("role")
    private String role;
    @SerializedName("companyId")
    private Long companyId;
    @SerializedName("accessToken")
    private String accessToken;
    @SerializedName("refreshToken")
    private String refreshToken;

    public Long getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
