package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class UpdateUserProfileRequest {

    @SerializedName("firstName")
    private final String firstName;
    @SerializedName("lastName")
    private final String lastName;
    @SerializedName("email")
    private final String email;
    @SerializedName("phone")
    private final String phone;
    @SerializedName("currentPassword")
    private final String currentPassword;

    public UpdateUserProfileRequest(String firstName, String lastName, String email, String phone,
                                    String currentPassword) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.currentPassword = currentPassword;
    }
}
