package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class ResetPasswordRequest {

    @SerializedName("token")
    private final String token;
    @SerializedName("newPassword")
    private final String newPassword;
    @SerializedName("confirmPassword")
    private final String confirmPassword;

    public ResetPasswordRequest(String token, String newPassword, String confirmPassword) {
        this.token = token;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}
