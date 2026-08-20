package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class ChangePasswordRequest {

    @SerializedName("currentPassword")
    private final String currentPassword;
    @SerializedName("newPassword")
    private final String newPassword;
    @SerializedName("confirmPassword")
    private final String confirmPassword;

    public ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }
}
