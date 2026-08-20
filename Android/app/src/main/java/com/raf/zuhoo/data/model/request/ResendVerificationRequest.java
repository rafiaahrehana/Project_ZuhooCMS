package com.raf.zuhoo.data.model.request;

import com.google.gson.annotations.SerializedName;

public class ResendVerificationRequest {

    @SerializedName("email")
    private final String email;

    public ResendVerificationRequest(String email) {
        this.email = email;
    }
}
